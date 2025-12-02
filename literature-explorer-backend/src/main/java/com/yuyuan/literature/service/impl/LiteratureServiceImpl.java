package com.yuyuan.literature.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuyuan.literature.common.exception.BusinessException;
import com.yuyuan.literature.common.request.PageResult;
import com.yuyuan.literature.common.result.ResultCode;
import com.yuyuan.literature.dto.BatchLiteratureImportRequest;
import com.yuyuan.literature.dto.LiteratureQueryRequest;
import com.yuyuan.literature.dto.LiteratureVO;
import com.yuyuan.literature.entity.Literature;
import com.yuyuan.literature.mapper.LiteratureMapper;
import com.yuyuan.literature.service.FileProcessingService;
import com.yuyuan.literature.service.LiteratureAiService;
import com.yuyuan.literature.service.LiteratureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletResponse;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 文献服务实现类
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiteratureServiceImpl extends ServiceImpl<LiteratureMapper, Literature> implements LiteratureService {

    private final FileProcessingService fileProcessingService;
    private final LiteratureAiService literatureAiService;
    
    // 虚拟线程池，用于并发处理文件
    private final Executor virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public Long createLiterature(MultipartFile file, String filePath, Integer contentLength) {
        Literature literature = new Literature();
        literature.setOriginalName(file.getOriginalFilename());
        literature.setFilePath(filePath);
        literature.setFileSize(file.getSize());
        literature.setFileType(FileUtil.extName(file.getOriginalFilename()).toLowerCase());
        literature.setContentLength(contentLength);
        literature.setStatus(Literature.Status.PROCESSING.getCode());

        this.save(literature);
        log.info("创建文献记录成功，ID: {}, 文件名: {}", literature.getId(), literature.getOriginalName());

        return literature.getId();
    }

    @Override
    public void updateReadingGuide(Long id, String readingGuide) {
        Literature literature = new Literature();
        literature.setId(id);
        literature.setReadingGuide(readingGuide);

        this.updateById(literature);
        log.info("更新文献阅读指南成功，ID: {}", id);
    }

    @Override
    public void updateClassification(Long id, List<String> tags, String description) {
        Literature literature = new Literature();
        literature.setId(id);
        literature.setTags(tags);
        literature.setDescription(description);
        literature.setStatus(Literature.Status.COMPLETED.getCode());

        this.updateById(literature);
        log.info("更新文献分类成功，ID: {}, 标签数量: {}", id, tags.size());
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Literature literature = new Literature();
        literature.setId(id);
        literature.setStatus(status);

        this.updateById(literature);
        log.info("更新文献状态成功，ID: {}, 状态: {}", id, status);
    }

    @Override
    public PageResult<LiteratureVO> pageLiteratures(LiteratureQueryRequest request) {
        // 创建分页对象
        Page<Literature> page = new Page<>(request.getPageNum(), request.getPageSize());

        // 执行分页查询
        IPage<Literature> pageResult = baseMapper.selectLiteraturePage(page, request);

        // 转换为 VO 对象
        List<LiteratureVO> voList = pageResult.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return PageResult.of(voList, pageResult.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public LiteratureVO getLiteratureDetail(Long id) {
        Literature literature = this.getById(id);
        if (literature == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "文献不存在");
        }

        return convertToDetailVO(literature);
    }

    /**
     * 转换为列表 VO（不包含完整阅读指南）
     */
    private LiteratureVO convertToVO(Literature literature) {
        LiteratureVO vo = new LiteratureVO();
        vo.setId(literature.getId());
        vo.setOriginalName(literature.getOriginalName());
        vo.setFileSize(literature.getFileSize());
        vo.setFileType(literature.getFileType());
        vo.setContentLength(literature.getContentLength());
        vo.setTags(literature.getTags());
        vo.setDescription(literature.getDescription());
        vo.setStatus(literature.getStatus());
        vo.setStatusDesc(getStatusDesc(literature.getStatus()));
        vo.setCreateTime(literature.getCreateTime());
        vo.setUpdateTime(literature.getUpdateTime());

        // 阅读指南摘要（截取前200个字符）
        if (StrUtil.isNotBlank(literature.getReadingGuide())) {
            String summary = literature.getReadingGuide().length() > 200
                    ? literature.getReadingGuide().substring(0, 200) + "..."
                    : literature.getReadingGuide();
            vo.setReadingGuideSummary(summary);
        }

        return vo;
    }

    /**
     * 转换为详情 VO（包含完整信息）
     */
    private LiteratureVO convertToDetailVO(Literature literature) {
        LiteratureVO vo = convertToVO(literature);
        // 详情页面可以返回完整的阅读指南
        vo.setReadingGuideSummary(literature.getReadingGuide());
        return vo;
    }

    @Override
    public void downloadLiteratureFile(Long id, HttpServletResponse response) {
        log.info("开始下载文献文件，ID: {}", id);

        // 查询文献信息
        Literature literature = this.getById(id);
        if (literature == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "文献不存在");
        }

        // 检查文件是否存在
        if (StrUtil.isBlank(literature.getFilePath())) {
            throw new BusinessException(ResultCode.FILE_NOT_EXIST, "文件路径为空");
        }

        // 处理文件路径，支持相对路径和绝对路径
        String filePath = literature.getFilePath();
        java.io.File file;

        // 如果是相对路径，则相对于项目根目录
        if (filePath.startsWith("./") || !filePath.startsWith("/")) {
            // 获取项目根目录
            String projectRoot = System.getProperty("user.dir");
            file = FileUtil.file(projectRoot, filePath);
        } else {
            // 绝对路径直接使用
            file = FileUtil.file(filePath);
        }

        if (!file.exists()) {
            throw new BusinessException(ResultCode.FILE_NOT_EXIST, "文件不存在: " + file.getAbsolutePath());
        }

        try {
            String fileName = literature.getOriginalName();
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            // 设置响应头
            response.setContentType("application/octet-stream");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition",
                    "attachment; filename*=UTF-8''" + encodedFileName);
            response.setHeader("Content-Length", String.valueOf(file.length()));

            // 下载文件
            try (OutputStream os = response.getOutputStream()) {
                FileUtil.writeToStream(file, os);
                os.flush();
            }

            log.info("文献文件下载成功，ID: {}, 文件名: {}, 文件大小: {} bytes",
                    id, fileName, file.length());
        } catch (Exception e) {
            log.error("下载文献文件失败，ID: {}", id, e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR, "文件下载失败: " + e.getMessage());
        }
    }

    @Override
    public SseEmitter batchImportLiterature(BatchLiteratureImportRequest request) {
        log.info("开始批量导入文献，文件数量: {}", request.getFiles().size());

        // 30分钟超时
        SseEmitter sseEmitter = new SseEmitter(30 * 60 * 1000L);
        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicInteger totalCount = new AtomicInteger(request.getFiles().size());
        AtomicInteger errorCount = new AtomicInteger(0);

        // 验证API Key
        literatureAiService.validateApiKey(request.getApiKey());
        
        // 发送批量开始事件
        sendSseEvent(sseEmitter, "batch_start", "{\"total\": " + totalCount.get() + ", \"message\": \"开始批量处理\"}");

        // 使用CompletableFuture + 虚拟线程池并发处理每个文件
        List<CompletableFuture<Void>> futures = request.getFiles().stream()
                .map(file -> CompletableFuture.runAsync(() -> {
                    try {
                        int fileIndex = request.getFiles().indexOf(file);
                        
                        // 发送文件开始处理事件
                        sendSseEvent(sseEmitter, "file_start", 
                                "{\"index\": " + fileIndex + ", \"filename\": \"" + file.getOriginalFilename() + "\", \"message\": \"开始处理文件\"}");
                        
                        // 保存文件
                        String filePath = fileProcessingService.saveFile(file);
                        
                        // 解析文件内容
                        String fileContent = fileProcessingService.extractFileContent(filePath);
                        
                        // 创建文献记录
                        Long literatureId = this.createLiterature(file, filePath, fileContent.length());
                        
                        // 发送文件保存成功事件
                        sendSseEvent(sseEmitter, "file_saved", 
                                "{\"index\": " + fileIndex + ", \"literatureId\": " + literatureId + ", \"message\": \"文件保存成功，开始生成阅读指南\"}");
                        
                        // 获取文件类型
                        String originalFilename = file.getOriginalFilename();
                        String fileType = "";
                        if (originalFilename != null && originalFilename.contains(".")) {
                            fileType = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
                        }

                        // 生成阅读指南（非流式，传递文件类型）
                        String readingGuide = literatureAiService.generateReadingGuideByFileType(request.getApiKey(), fileContent, fileType);
                        
                        // 更新阅读指南
                        if (StrUtil.isNotBlank(readingGuide)) {
                            this.updateReadingGuide(literatureId, readingGuide);
                            
                            // 异步生成分类
                            literatureAiService.generateClassificationWithVirtualThread(
                                    request.getApiKey(), readingGuide, literatureId, this);
                        } else {
                            this.updateStatus(literatureId, Literature.Status.COMPLETED.getCode());
                        }
                        
                        // 发送文件完成事件
                        int completed = completedCount.incrementAndGet();
                        sendSseEvent(sseEmitter, "file_complete", 
                                "{\"index\": " + fileIndex + ", \"literatureId\": " + literatureId + ", \"completed\": " + completed + ", \"total\": " + totalCount.get() + ", \"message\": \"文件处理完成\"}");
                        
                        // 检查是否所有文件都处理完成
                        if (completed == totalCount.get()) {
                            sendSseEvent(sseEmitter, "batch_complete", 
                                    "{\"message\": \"批量处理完成\", \"total\": " + totalCount.get() + ", \"errors\": " + errorCount.get() + "}");
                            sseEmitter.complete();
                        }
                        
                    } catch (Exception e) {
                        log.error("处理文件失败，文件名: {}", file.getOriginalFilename(), e);
                        
                        try {
                            // 发送文件失败事件
                            int fileIndex = request.getFiles().indexOf(file);
                            int completed = completedCount.incrementAndGet();
                            int errors = errorCount.incrementAndGet();
                            
                            sendSseEvent(sseEmitter, "file_error", 
                                    "{\"index\": " + fileIndex + ", \"filename\": \"" + file.getOriginalFilename() + "\", \"error\": \"" + e.getMessage() + "\", \"completed\": " + completed + ", \"total\": " + totalCount.get() + "}");
                            
                            // 检查是否所有文件都处理完成（包括失败的）
                            if (completed == totalCount.get()) {
                                sendSseEvent(sseEmitter, "batch_complete", 
                                        "{\"message\": \"批量处理完成（部分失败）\", \"total\": " + totalCount.get() + ", \"errors\": " + errors + "}");
                                sseEmitter.complete();
                            }
                        } catch (Exception sendException) {
                            log.error("发送错误事件失败", sendException);
                        }
                    }
                }, virtualThreadExecutor))
                .collect(Collectors.toList());
        
        // 设置SSE回调
        sseEmitter.onCompletion(() -> {
            log.info("批量导入SSE连接完成");
        });
        
        sseEmitter.onError((throwable) -> {
            log.error("批量导入SSE连接错误", throwable);
        });
        
        sseEmitter.onTimeout(() -> {
            log.warn("批量导入SSE连接超时");
        });
        
        return sseEmitter;
    }

    /**
     * 安全发送SSE事件
     */
    private void sendSseEvent(SseEmitter sseEmitter, String eventName, String data) {
        try {
            sseEmitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (Exception e) {
            log.warn("发送SSE事件失败: {} - {}", eventName, e.getMessage());
        }
    }

    /**
     * 导出文献阅读指南为Markdown
     */
    @Override
    public void exportReadingGuideMarkdown(Long id, HttpServletResponse response) {
        try {
            // 查询文献信息
            Literature literature = getById(id);
            if (literature == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "文献不存在");
            }

            // 检查是否有阅读指南
            if (StrUtil.isBlank(literature.getReadingGuide())) {
                throw new BusinessException(ResultCode.VALIDATION_FAILED, "该文献暂无阅读指南，无法导出");
            }

            // 生成Markdown内容
            String markdownContent = generateMarkdownContent(literature);

            // 设置响应头
            String fileName = literature.getOriginalName();
            if (fileName.contains(".")) {
                fileName = fileName.substring(0, fileName.lastIndexOf("."));
            }
            fileName += "_阅读指南.md";

            response.setContentType("text/markdown; charset=utf-8");
            response.setHeader("Content-Disposition",
                "attachment; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8) + "\"");
            response.setHeader("Content-Length", String.valueOf(markdownContent.getBytes(StandardCharsets.UTF_8).length));

            // 写入响应流
            try (OutputStream outputStream = response.getOutputStream()) {
                outputStream.write(markdownContent.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }

            log.info("成功导出文献阅读指南，ID: {}, 文件名: {}", id, fileName);

        } catch (BusinessException e) {
            log.error("导出文献阅读指南失败，ID: {}, 错误: {}", id, e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            throw e;
        } catch (Exception e) {
            log.error("导出文献阅读指南异常，ID: {}", id, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            throw new BusinessException(ResultCode.ERROR, "导出失败: " + e.getMessage());
        }
    }

    /**
     * 生成Markdown内容
     */
    private String generateMarkdownContent(Literature literature) {
        StringBuilder markdown = new StringBuilder();

        // 文档标题
        String title = literature.getOriginalName();
        if (title.contains(".")) {
            title = title.substring(0, title.lastIndexOf("."));
        }
        markdown.append("# ").append(title).append(" - 阅读指南\n\n");

        // 文档信息
        markdown.append("## 📄 文档信息\n\n");
        markdown.append("- **文件名**: ").append(literature.getOriginalName()).append("\n");
        markdown.append("- **文件类型**: ").append(literature.getFileType()).append("\n");
        markdown.append("- **文件大小**: ").append(formatFileSize(literature.getFileSize())).append("\n");
        markdown.append("- **内容长度**: ").append(literature.getContentLength()).append(" 字符\n");

        if (StrUtil.isNotBlank(literature.getDescription())) {
            markdown.append("- **AI 描述**: ").append(literature.getDescription()).append("\n");
        }

        if (literature.getTags() != null && !literature.getTags().isEmpty()) {
            markdown.append("- **标签**: ");
            markdown.append(literature.getTags().stream()
                .map(tag -> "`" + tag + "`")
                .collect(Collectors.joining(", ")));
            markdown.append("\n");
        }

        markdown.append("- **生成时间**: ").append(literature.getUpdateTime()).append("\n\n");

        // 阅读指南内容
        markdown.append("## 📖 AI 生成的阅读指南\n\n");
        markdown.append(literature.getReadingGuide());

        // 添加页脚
        markdown.append("\n\n---\n\n");
        markdown.append("*本阅读指南由 [文献助手](https://github.com/your-repo/literature-assistant) 基于 Kimi AI 自动生成*\n");

        return markdown.toString();
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(Long size) {
        if (size == null || size == 0) {
            return "0 B";
        }

        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        double fileSize = size.doubleValue();

        while (fileSize >= 1024 && unitIndex < units.length - 1) {
            fileSize /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", fileSize, units[unitIndex]);
    }

    /**
     * 获取状态描述
     */
    private String getStatusDesc(Integer status) {
        if (status == null) {
            return "未知";
        }

        return switch (status) {
            case 0 -> "处理中";
            case 1 -> "已完成";
            case 2 -> "处理失败";
            default -> "未知";
        };
    }

    @Override
    public void deleteLiterature(Long id) {
        log.info("开始删除文献，ID: {}", id);

        // 检查文献是否存在
        Literature literature = this.getById(id);
        if (literature == null) {
            log.warn("文献不存在，ID: {}", id);
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "文献不存在");
        }

        // 执行逻辑删除
        boolean result = this.removeById(id);
        if (!result) {
            log.error("删除文献失败，ID: {}", id);
            throw new BusinessException(ResultCode.ERROR, "删除文献失败");
        }

        log.info("成功删除文献，ID: {}, 文件名: {}", id, literature.getOriginalName());
    }

    @Override
    public void batchDeleteLiterature(List<Long> ids) {
        log.info("开始批量删除文献，ID数量: {}", ids.size());

        if (ids == null || ids.isEmpty()) {
            log.warn("批量删除文献失败：ID列表为空");
            throw new BusinessException(ResultCode.VALIDATION_FAILED, "文献ID列表不能为空");
        }

        // 检查所有文献是否存在
        List<Literature> existingLiteratures = this.listByIds(ids);
        if (existingLiteratures.size() != ids.size()) {
            log.warn("部分文献不存在，总数: {}, 存在: {}", ids.size(), existingLiteratures.size());
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "部分文献不存在，请检查ID列表");
        }

        // 执行批量逻辑删除
        boolean result = this.removeByIds(ids);
        if (!result) {
            log.error("批量删除文献失败，ID数量: {}", ids.size());
            throw new BusinessException(ResultCode.ERROR, "批量删除文献失败");
        }

        // 记录删除的文献信息
        List<String> deletedFileNames = existingLiteratures.stream()
                .map(Literature::getOriginalName)
                .collect(Collectors.toList());

        log.info("成功批量删除文献，数量: {}, 文件名: {}",
                existingLiteratures.size(), String.join(", ", deletedFileNames));
    }
}
