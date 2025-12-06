package com.yuyuan.literature.controller;

import cn.hutool.core.util.StrUtil;
import com.yuyuan.literature.common.request.PageResult;
import com.yuyuan.literature.common.result.Result;
import com.yuyuan.literature.dto.BatchLiteratureImportRequest;
import com.yuyuan.literature.dto.LiteratureQueryRequest;
import com.yuyuan.literature.dto.LiteratureQuestionRequest;
import com.yuyuan.literature.dto.LiteratureVO;
import com.yuyuan.literature.entity.Literature;
import com.yuyuan.literature.service.DocumentExportService;
import com.yuyuan.literature.service.FileProcessingService;
import com.yuyuan.literature.service.LiteratureAiService;
import com.yuyuan.literature.service.LiteratureQAService;
import com.yuyuan.literature.service.LiteratureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 文献助手控制器
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/literature")
@Tag(name = "文献助手", description = "文献分析和阅读指南生成")
@RequiredArgsConstructor
@Validated
public class LiteratureController {

    private final FileProcessingService fileProcessingService;
    private final LiteratureAiService literatureAiService;
    private final LiteratureService literatureService;

    @Autowired
    private LiteratureQAService literatureQAService;

    @Autowired
    private DocumentExportService documentExportService;

    /**
     * 生成文献阅读指南
     *
     * @param file   上传的文献文件
     * @param apiKey Kimi AI API Key
     * @return SSE 流式响应
     */
    @PostMapping(value = "/generate-guide", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "生成文献阅读指南", description = "上传文献文件，AI 生成阅读指南（SSE 流式响应）")
    public SseEmitter generateReadingGuide(
            @Parameter(description = "文献文件（支持 PDF、Word、Markdown）", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "Kimi AI API Key", required = true) @RequestParam("apiKey") @NotBlank(message = "API Key 不能为空") String apiKey) {

        log.info("开始生成文献阅读指南，文件名: {}, 文件大小: {} bytes",
                file.getOriginalFilename(), file.getSize());

        // 创建 SSE 发射器，设置超时时间为 10 分钟
        SseEmitter sseEmitter = new SseEmitter(TimeUnit.MINUTES.toMillis(10));

        Long literatureId = null;
        String filePath = null;

        try {
            // 验证 API Key
            literatureAiService.validateApiKey(apiKey);

            // 发送开始事件
            sseEmitter.send(SseEmitter.event()
                    .name("start")
                    .data("开始处理文献文件..."));

            // 保存文件
            filePath = fileProcessingService.saveFile(file);

            // 发送文件保存成功事件
            sseEmitter.send(SseEmitter.event()
                    .name("progress")
                    .data("文件保存成功，开始解析内容..."));

            // 解析文件内容
            String fileContent = fileProcessingService.extractFileContent(filePath);

            // 创建文献记录
            literatureId = literatureService.createLiterature(file, filePath, fileContent.length());

            // 发送内容解析成功事件
            sseEmitter.send(SseEmitter.event()
                    .name("progress")
                    .data("内容解析成功，开始生成阅读指南..."));

            // 创建内容收集器
            StringBuilder readingGuideBuilder = new StringBuilder();

            // 获取文件类型
            String originalFilename = file.getOriginalFilename();
            String fileType = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileType = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            }

            // 异步生成阅读指南（带内容收集，传递文件类型）
            literatureAiService.generateReadingGuideStream(apiKey, fileContent, fileType, sseEmitter,
                    readingGuideBuilder);

            // 设置 SSE 完成回调，在阅读指南完成后启动分类
            final String finalFilePath = filePath;
            final Long finalLiteratureId = literatureId;

            sseEmitter.onCompletion(() -> {
                log.info("阅读指南生成完成，开始后台分类处理");

                // 保存阅读指南到数据库
                String readingGuide = readingGuideBuilder.toString();
                if (StrUtil.isNotBlank(readingGuide)) {
                    literatureService.updateReadingGuide(finalLiteratureId, readingGuide);

                    // 使用虚拟线程进行后台分类
                    literatureAiService.generateClassificationWithVirtualThread(
                            apiKey, readingGuide, finalLiteratureId, literatureService);
                } else {
                    log.warn("阅读指南内容为空，跳过分类处理");
                    literatureService.updateStatus(finalLiteratureId, Literature.Status.COMPLETED.getCode());
                }
            });

            sseEmitter.onError((throwable) -> {
                log.error("文献阅读指南生成失败，文件: {}", file.getOriginalFilename(), throwable);
                if (finalLiteratureId != null) {
                    literatureService.updateStatus(finalLiteratureId, Literature.Status.FAILED.getCode());
                }
            });

            sseEmitter.onTimeout(() -> {
                log.warn("文献阅读指南生成超时，文件: {}", file.getOriginalFilename());
                if (finalLiteratureId != null) {
                    literatureService.updateStatus(finalLiteratureId, Literature.Status.FAILED.getCode());
                }
            });

        } catch (Exception e) {
            log.error("处理文献文件失败", e);
            if (literatureId != null) {
                literatureService.updateStatus(literatureId, Literature.Status.FAILED.getCode());
            }
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

        return sseEmitter;
    }

    /**
     * 分页查询文献
     */
    @PostMapping("/page")
    @Operation(summary = "分页查询文献", description = "根据条件分页查询文献列表")
    public Result<PageResult<LiteratureVO>> pageLiteratures(
            @Valid @RequestBody LiteratureQueryRequest request) {

        log.info("分页查询文献，条件: {}", request);

        PageResult<LiteratureVO> result = literatureService.pageLiteratures(request);
        return Result.success(result);
    }

    /**
     * 获取文献详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取文献详情", description = "根据ID获取文献完整信息")
    public Result<LiteratureVO> getLiteratureDetail(
            @Parameter(description = "文献ID", required = true) @PathVariable @NotNull(message = "文献ID不能为空") Long id) {

        log.info("获取文献详情，ID: {}", id);

        LiteratureVO result = literatureService.getLiteratureDetail(id);
        return Result.success(result);
    }

    /**
     * 下载文献文件
     */
    @GetMapping("/{id}/download")
    @Operation(summary = "下载文献文件", description = "根据文献ID下载对应的原始文件")
    public void downloadLiteratureFile(
            @Parameter(description = "文献ID", required = true) @PathVariable @NotNull(message = "文献ID不能为空") Long id,
            jakarta.servlet.http.HttpServletResponse response) {

        log.info("下载文献文件，ID: {}", id);
        literatureService.downloadLiteratureFile(id, response);
    }

    /**
     * 批量导入文献
     */
    @PostMapping(value = "/batch-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "批量导入文献", description = "批量上传文献文件，AI生成阅读指南并实时返回处理状态")
    public SseEmitter batchImportLiterature(
            @Parameter(description = "文献文件列表", required = true) @RequestPart("files") List<MultipartFile> files,
            @Parameter(description = "Kimi AI API Key", required = true) @RequestParam("apiKey") @NotBlank(message = "API Key 不能为空") String apiKey) {

        log.info("开始批量导入文献，文件数量: {}", files.size());

        BatchLiteratureImportRequest request = new BatchLiteratureImportRequest();
        request.setFiles(files);
        request.setApiKey(apiKey);

        return literatureService.batchImportLiterature(request);
    }

    /**
     * 导出文献阅读指南为Markdown
     */
    @GetMapping("/{id}/export-reading-guide")
    @Operation(summary = "导出文献阅读指南", description = "根据文献ID导出阅读指南为Markdown格式")
    public void exportReadingGuideMarkdown(
            @Parameter(description = "文献ID", required = true) @PathVariable @NotNull(message = "文献ID不能为空") Long id,
            jakarta.servlet.http.HttpServletResponse response) {

        log.info("导出文献阅读指南，ID: {}", id);
        literatureService.exportReadingGuideMarkdown(id, response);
    }

    /**
     * 删除文献
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除文献", description = "根据ID删除文献记录（逻辑删除）")
    public Result<Void> deleteLiterature(
            @Parameter(description = "文献ID", required = true) @PathVariable @NotNull(message = "文献ID不能为空") Long id) {

        log.info("删除文献，ID: {}", id);

        literatureService.deleteLiterature(id);
        return Result.success();
    }

    /**
     * 批量删除文献
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除文献", description = "根据ID列表批量删除文献记录（逻辑删除）")
    public Result<Void> batchDeleteLiterature(
            @Parameter(description = "文献ID列表", required = true) @RequestBody @NotNull(message = "文献ID列表不能为空") List<Long> ids) {

        log.info("批量删除文献，ID数量: {}", ids.size());

        literatureService.batchDeleteLiterature(ids);
        return Result.success();
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查文献助手服务状态")
    public String health() {
        return "Literature Assistant is running!";
    }

    /**
     * 文献问答接口（SSE 流式响应）
     */
    @PostMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "文献问答", description = "基于文献内容回答用户问题（支持单文献和跨文献检索）")
    public SseEmitter askQuestion(
            @Parameter(description = "问答请求", required = true) @Valid @RequestBody LiteratureQuestionRequest request) {

        log.info("收到文献问答请求，问题: {}, 跨文献: {}, 文献ID: {}",
                request.getQuestion(), request.getCrossDoc(), request.getLiteratureId());

        // 验证 API Key
        literatureAiService.validateApiKey(request.getApiKey());

        // 处理问答请求
        return literatureQAService.answerQuestion(request);
    }

    /**
     * 导出文献阅读指南为Word文档
     */
    @GetMapping("/{id}/export-reading-guide-word")
    @Operation(summary = "导出文献阅读指南为Word文档", description = "根据文献ID导出阅读指南为Word格式")
    public void exportReadingGuideWord(
            @Parameter(description = "文献ID", required = true) @PathVariable @NotNull(message = "文献ID不能为空") Long id,
            jakarta.servlet.http.HttpServletResponse response) {

        log.info("导出文献阅读指南为Word文档，文献ID: {}", id);

        try {
            documentExportService.exportReadingGuideToWord(id, response);
        } catch (IOException e) {
            log.error("导出Word文档失败，文献ID: {}", id, e);
            response.setStatus(500);
            try {
                response.getWriter().write("导出Word文档失败: " + e.getMessage());
            } catch (IOException ioException) {
                log.error("写入错误响应失败", ioException);
            }
        }
    }
}