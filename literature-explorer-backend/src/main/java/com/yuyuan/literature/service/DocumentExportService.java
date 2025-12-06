package com.yuyuan.literature.service;

import com.yuyuan.literature.entity.Literature;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文档导出服务
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Slf4j
@Service
public class DocumentExportService {

    @Autowired
    private MarkdownToWordConverter markdownToWordConverter;

    @Autowired
    private LiteratureService literatureService;

    /**
     * 导出文献阅读指南为Word文档
     *
     * @param literatureId 文献ID
     * @param response HTTP响应
     * @throws IOException 文件操作异常
     */
    public void exportReadingGuideToWord(Long literatureId, HttpServletResponse response) throws IOException {
        log.info("开始导出文献阅读指南为Word文档，文献ID: {}", literatureId);

        // 1. 获取文献信息
        Literature literature = literatureService.getById(literatureId);
        if (literature == null) {
            log.error("文献不存在，ID: {}", literatureId);
            response.setStatus(404);
            response.getWriter().write("文献不存在");
            return;
        }

        // 2. 检查阅读指南内容
        String readingGuide = literature.getReadingGuide();
        if (readingGuide == null || readingGuide.trim().isEmpty()) {
            log.error("文献阅读指南为空，无法导出，文献ID: {}", literatureId);
            response.setStatus(400);
            response.getWriter().write("文献阅读指南为空，无法导出");
            return;
        }

        // 3. 转换Markdown为Word文档
        XWPFDocument document = markdownToWordConverter.convertToWord(readingGuide);

        // 4. 设置响应头
        String fileName = literature.getOriginalName() + "_阅读指南.docx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString());

        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + "\"; filename*=" + encodedFileName);

        // 5. 写入响应流
        try (OutputStream outputStream = response.getOutputStream()) {
            document.write(outputStream);
            outputStream.flush();
            log.info("文献阅读指南Word文档导出成功，文献ID: {}", literatureId);
        } catch (IOException e) {
            log.error("导出Word文档失败，文献ID: {}", literatureId, e);
            throw e;
        } finally {
            // 6. 关闭文档
            try {
                document.close();
            } catch (IOException e) {
                log.error("关闭Word文档失败", e);
            }
        }
    }

    /**
     * 验证文献是否可以导出
     *
     * @param literatureId 文献ID
     * @return 验证结果
     */
    public ExportValidationResult validateForExport(Long literatureId) {
        ExportValidationResult result = new ExportValidationResult();

        Literature literature = literatureService.getById(literatureId);
        if (literature == null) {
            result.setValid(false);
            result.setErrorMessage("文献不存在");
            return result;
        }

        String readingGuide = literature.getReadingGuide();
        if (readingGuide == null || readingGuide.trim().isEmpty()) {
            result.setValid(false);
            result.setErrorMessage("文献阅读指南为空，无法导出");
            return result;
        }

        // 检查内容长度限制
        if (readingGuide.length() > 100000) {
            result.setValid(false);
            result.setErrorMessage("文档内容过大，无法导出");
            return result;
        }

        result.setValid(true);
        return result;
    }

    /**
     * 导出验证结果类
     */
    public static class ExportValidationResult {
        private boolean valid;
        private String errorMessage;

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}