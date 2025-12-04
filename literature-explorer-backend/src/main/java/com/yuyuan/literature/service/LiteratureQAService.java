package com.yuyuan.literature.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yuyuan.literature.common.exception.BusinessException;
import com.yuyuan.literature.common.result.ResultCode;
import com.yuyuan.literature.dto.KimiChatRequest;
import com.yuyuan.literature.dto.LiteratureContext;
import com.yuyuan.literature.dto.LiteratureQuestionRequest;
import com.yuyuan.literature.entity.Literature;
import com.yuyuan.literature.mapper.LiteratureMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 文献问答服务
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Slf4j
@Service
public class LiteratureQAService {

    @Value("${literature.ai.base-url:https://api.moonshot.cn/v1}")
    private String baseUrl;

    @Value("${literature.ai.model:moonshot-v1-8k}")
    private String model;

    @Value("${literature.ai.max-tokens:4000}")
    private Integer maxTokens;

    @Value("${literature.ai.temperature:0.7}")
    private Double temperature;

    @Value("${literature.ai.qa-prompt-file}")
    private String qaPromptFile;

    private final OkHttpClient httpClient;
    private final ResourceLoader resourceLoader;
    private final LiteratureMapper literatureMapper;
    private final LiteratureService literatureService;

    // 缓存问答提示词
    private String cachedQAPrompt;

    // 最大上下文字符数限制（防止超长）
    private static final int MAX_CONTEXT_LENGTH = 20000;

    public LiteratureQAService(ResourceLoader resourceLoader, LiteratureMapper literatureMapper,
            LiteratureService literatureService) {
        this.resourceLoader = resourceLoader;
        this.literatureMapper = literatureMapper;
        this.literatureService = literatureService;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder()
                            .header("Accept-Charset", "UTF-8");
                    return chain.proceed(requestBuilder.build());
                })
                .build();
    }

    /**
     * 获取问答系统提示词（带缓存）
     */
    private String getQAPrompt() {
        if (cachedQAPrompt == null) {
            cachedQAPrompt = loadPromptFromFile(qaPromptFile);
        }
        return cachedQAPrompt;
    }

    /**
     * 从资源文件加载提示词内容
     */
    private String loadPromptFromFile(String filePath) {
        String resourcePath = filePath.replace("classpath:", "");

        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                log.error("无法在 classpath 中找到资源文件: {}", resourcePath);
                throw new BusinessException(ResultCode.ERROR, "系统提示词文件不存在: " + filePath);
            }

            try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                    BufferedReader bufferedReader = new BufferedReader(reader)) {

                StringBuilder contentBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    contentBuilder.append(line).append(System.lineSeparator());
                }

                String content = contentBuilder.toString();

                if (StrUtil.isBlank(content)) {
                    throw new BusinessException(ResultCode.ERROR, "系统提示词文件内容为空: " + filePath);
                }

                log.info("成功加载问答系统提示词文件: {}, 内容长度: {}", filePath, content.length());
                return content;
            }
        } catch (IOException e) {
            log.error("读取系统提示词文件失败: {}", filePath, e);
            throw new BusinessException(ResultCode.ERROR, "读取系统提示词文件失败: " + e.getMessage());
        }
    }

    /**
     * 处理问答请求（SSE 流式响应）
     */
    public SseEmitter answerQuestion(LiteratureQuestionRequest request) {
        log.info("开始处理文献问答，问题: {}, 跨文献: {}", request.getQuestion(), request.getCrossDoc());

        // 创建 SSE 发射器，设置超时时间为 10 分钟
        SseEmitter sseEmitter = new SseEmitter(TimeUnit.MINUTES.toMillis(10));

        CompletableFuture.runAsync(() -> {
            try {
                // 发送开始事件
                sseEmitter.send(SseEmitter.event()
                        .name("start")
                        .data("开始处理问题..."));

                // 组装上下文
                List<LiteratureContext> contexts = assembleContexts(request);

                if (contexts == null || contexts.isEmpty()) {
                    sseEmitter.send(SseEmitter.event()
                            .name("error")
                            .data("未找到相关文献内容，请确保文献已完成AI分析"));
                    sseEmitter.complete();
                    return;
                }

                // 发送进度事件
                sseEmitter.send(SseEmitter.event()
                        .name("progress")
                        .data(String.format("找到 %d 篇相关文献，正在生成答案...", contexts.size())));

                // 构建提示词
                String prompt = buildQAPrompt(contexts, request.getQuestion());

                // 流式生成答案
                streamAnswer(request.getApiKey(), prompt, sseEmitter);

            } catch (Exception e) {
                log.error("处理问答失败", e);
                try {
                    sseEmitter.send(SseEmitter.event()
                            .name("error")
                            .data("处理失败: " + e.getMessage()));
                    sseEmitter.completeWithError(e);
                } catch (IOException ioException) {
                    log.error("发送错误消息失败", ioException);
                    sseEmitter.completeWithError(ioException);
                }
            }
        });

        return sseEmitter;
    }

    /**
     * 组装上下文
     */
    private List<LiteratureContext> assembleContexts(LiteratureQuestionRequest request) {
        if (request.getCrossDoc()) {
            // 跨文献检索
            String keyword = StrUtil.isNotBlank(request.getKeyword())
                    ? request.getKeyword()
                    : extractKeywords(request.getQuestion());

            log.info("跨文献检索，关键词: {}, topK: {}", keyword, request.getTopK());
            return literatureMapper.searchRelevantLiteratures(keyword, request.getTopK());
        } else {
            // 单文献问答
            if (request.getLiteratureId() == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "单文献问答时必须提供文献ID");
            }

            Literature literature = literatureService.getById(request.getLiteratureId());
            if (literature == null) {
                throw new BusinessException(ResultCode.DATA_NOT_EXIST, "文献不存在");
            }

            if (StrUtil.isBlank(literature.getReadingGuide()) && StrUtil.isBlank(literature.getDescription())) {
                throw new BusinessException(ResultCode.VALIDATION_FAILED, "该文献暂无AI分析内容，无法回答问题");
            }

            LiteratureContext context = LiteratureContext.builder()
                    .literatureId(literature.getId())
                    .title(literature.getOriginalName())
                    .description(literature.getDescription())
                    .readingGuide(literature.getReadingGuide())
                    .tags(literature.getTags() != null ? String.join(", ", literature.getTags()) : "")
                    .relevanceScore(1.0)
                    .build();

            return List.of(context);
        }
    }

    /**
     * 从问题中提取关键词（简单实现）
     */
    private String extractKeywords(String question) {
        // 移除常见疑问词和停用词
        String[] stopWords = { "什么", "怎么", "如何", "为什么", "哪些", "是否", "能否", "的", "了", "吗", "呢" };
        String cleaned = question;
        for (String stopWord : stopWords) {
            cleaned = cleaned.replace(stopWord, " ");
        }
        return cleaned.trim();
    }

    /**
     * 构建问答提示词
     */
    private String buildQAPrompt(List<LiteratureContext> contexts, String question) {
        StringBuilder promptBuilder = new StringBuilder();

        // 构建用户消息
        promptBuilder.append("**用户问题：**\n").append(question).append("\n\n");
        promptBuilder.append("**可用的文献上下文：**\n\n");

        int contextIndex = 1;
        int totalLength = 0;

        for (LiteratureContext context : contexts) {
            StringBuilder contextSection = new StringBuilder();
            contextSection.append("---\n");
            contextSection.append(String.format("### 文献 %d: %s (ID: %d)\n\n",
                    contextIndex++, context.getTitle(), context.getLiteratureId()));

            if (StrUtil.isNotBlank(context.getTags())) {
                contextSection.append("**标签**: ").append(context.getTags()).append("\n\n");
            }

            if (StrUtil.isNotBlank(context.getDescription())) {
                contextSection.append("**描述**: ").append(context.getDescription()).append("\n\n");
            }

            if (StrUtil.isNotBlank(context.getReadingGuide())) {
                contextSection.append("**阅读指南**:\n").append(context.getReadingGuide()).append("\n\n");
            }

            // 检查是否超过最大长度
            if (totalLength + contextSection.length() > MAX_CONTEXT_LENGTH) {
                log.warn("上下文长度超过限制，已截断部分内容");
                break;
            }

            promptBuilder.append(contextSection);
            totalLength += contextSection.length();
        }

        promptBuilder.append("---\n\n");
        promptBuilder.append("请基于以上文献内容，回答用户的问题。");

        return promptBuilder.toString();
    }

    /**
     * 流式生成答案
     */
    private void streamAnswer(String apiKey, String prompt, SseEmitter sseEmitter) {
        try {
            // 构建请求
            KimiChatRequest chatRequest = new KimiChatRequest();
            chatRequest.setModel(model);
            chatRequest.setMaxTokens(maxTokens);
            chatRequest.setTemperature(temperature);
            chatRequest.setStream(true);

            chatRequest.setMessages(Arrays.asList(
                    new KimiChatRequest.Message("system", getQAPrompt()),
                    new KimiChatRequest.Message("user", prompt)));

            String requestBody = JSONUtil.toJsonStr(chatRequest);

            log.info("发送 Kimi AI 问答请求，模型: {}", model);

            // 构建 HTTP 请求
            Request request = new Request.Builder()
                    .url(baseUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Accept", "text/event-stream")
                    .header("Accept-Charset", "UTF-8")
                    .header("Cache-Control", "no-cache")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json; charset=utf-8")))
                    .build();

            // 创建 EventSource 监听器
            EventSourceListener listener = new EventSourceListener() {
                @Override
                public void onOpen(EventSource eventSource, Response response) {
                    log.info("Kimi AI SSE 连接已建立");
                }

                @Override
                public void onEvent(EventSource eventSource, String id, String type, String data) {
                    try {
                        // 检查是否为结束标记
                        if ("[DONE]".equals(data)) {
                            sseEmitter.send(SseEmitter.event()
                                    .name("complete")
                                    .data("回答完成"));
                            sseEmitter.complete();
                            return;
                        }

                        // 跳过空数据
                        if (StrUtil.isBlank(data)) {
                            return;
                        }

                        // 解析 JSON 响应
                        JSONObject jsonData = JSONUtil.parseObj(data);
                        String content = extractContentFromResponse(jsonData);

                        if (content != null) {
                            // 发送内容片段到前端
                            sseEmitter.send(SseEmitter.event()
                                    .name("content")
                                    .data(content.replace("\n", "<empty-line>").replace(" ", "<empty-space>")));
                        }

                    } catch (Exception e) {
                        log.warn("处理 SSE 事件失败", e);
                    }
                }

                @Override
                public void onClosed(EventSource eventSource) {
                    log.info("Kimi AI SSE 连接已关闭");
                }

                @Override
                public void onFailure(EventSource eventSource, Throwable t, Response response) {
                    log.error("Kimi AI SSE 连接失败", t);
                    try {
                        String errorMsg = "AI 服务连接失败";
                        if (response != null) {
                            errorMsg += ": " + response.code() + " - " + response.message();
                            if (response.body() != null) {
                                errorMsg += " - " + response.body().string();
                            }
                        }

                        sseEmitter.send(SseEmitter.event()
                                .name("error")
                                .data(errorMsg));
                        sseEmitter.completeWithError(new BusinessException(errorMsg));
                    } catch (IOException e) {
                        log.error("发送错误消息失败", e);
                        sseEmitter.completeWithError(t);
                    }
                }
            };

            // 创建 EventSource 并开始监听
            EventSource eventSource = EventSources.createFactory(httpClient)
                    .newEventSource(request, listener);

            // 设置 SSE 发射器的回调
            sseEmitter.onCompletion(() -> {
                log.info("SSE 连接完成，关闭 EventSource");
                eventSource.cancel();
            });

            sseEmitter.onError((throwable) -> {
                log.error("SSE 连接错误，关闭 EventSource", throwable);
                eventSource.cancel();
            });

            sseEmitter.onTimeout(() -> {
                log.warn("SSE 连接超时，关闭 EventSource");
                eventSource.cancel();
            });

        } catch (Exception e) {
            log.error("生成答案失败", e);
            try {
                sseEmitter.send(SseEmitter.event()
                        .name("error")
                        .data("生成答案失败: " + e.getMessage()));
                sseEmitter.completeWithError(e);
            } catch (IOException ioException) {
                log.error("发送错误消息失败", ioException);
                sseEmitter.completeWithError(ioException);
            }
        }
    }

    /**
     * 从响应中提取内容
     */
    private String extractContentFromResponse(JSONObject jsonData) {
        try {
            JSONObject delta = jsonData.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("delta");

            if (delta.containsKey("content")) {
                Object contentObj = delta.get("content");
                if (contentObj != null) {
                    return contentObj.toString();
                }
            }

            return null;

        } catch (Exception e) {
            log.debug("提取内容失败: {}", jsonData, e);
            return null;
        }
    }
}
