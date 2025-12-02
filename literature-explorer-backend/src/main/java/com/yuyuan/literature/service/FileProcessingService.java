package com.yuyuan.literature.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.yuyuan.literature.common.exception.BusinessException;
import com.yuyuan.literature.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 文件处理服务
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Slf4j
@Service
public class FileProcessingService {

    @Value("${literature.file.upload-path:./uploads/documents}")
    private String uploadPath;

    @Value("${literature.file.allowed-extensions:pdf,doc,docx,md,markdown,xls,xlsx}")
    private String allowedExtensions;

    @Value("${literature.file.max-file-size:10MB}")
    private String maxFileSize;

    /**
     * 保存上传的文件并返回文件路径
     *
     * @param file 上传的文件
     * @return 保存后的文件路径
     */
    public String saveFile(MultipartFile file) {
        try {
            // 验证文件
            validateFile(file);

            // 确保上传目录存在
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = FilenameUtils.getExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID().toString() + "." + extension;
            
            // 保存文件
            Path filePath = uploadDir.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath);
            
            log.info("文件保存成功: {}", filePath.toString());
            return filePath.toString();
            
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR);
        }
    }

    /**
     * 解析文件内容
     *
     * @param filePath 文件路径
     * @return 文件内容
     */
    public String extractFileContent(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                throw new BusinessException(ResultCode.FILE_NOT_EXIST);
            }

            String extension = FilenameUtils.getExtension(filePath).toLowerCase();

            return switch (extension) {
                case "pdf" -> extractPdfContent(file);
                case "doc" -> extractDocContent(file);
                case "docx" -> extractDocxContent(file);
                case "md", "markdown" -> extractMarkdownContent(file);
                case "xls" -> extractExcelContent(file, true);
                case "xlsx" -> extractExcelContent(file, false);
                default -> throw new BusinessException(ResultCode.FILE_TYPE_NOT_SUPPORTED);
            };
            
        } catch (Exception e) {
            log.error("文件内容解析失败: {}", filePath, e);
            if (e instanceof BusinessException) {
                throw (BusinessException) e;
            }
            throw new BusinessException("文件内容解析失败: " + e.getMessage());
        }
    }

    /**
     * 验证上传的文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传的文件为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (StrUtil.isBlank(originalFilename)) {
            throw new BusinessException("文件名不能为空");
        }

        // 验证文件扩展名
        String extension = FilenameUtils.getExtension(originalFilename).toLowerCase();
        List<String> allowedExtList = Arrays.asList(allowedExtensions.split(","));
        if (!allowedExtList.contains(extension)) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_SUPPORTED, 
                "只支持以下文件类型: " + allowedExtensions);
        }

        // 验证文件大小
        long maxSize = parseFileSize(maxFileSize);
        if (file.getSize() > maxSize) {
            throw new BusinessException(ResultCode.FILE_SIZE_EXCEEDED, 
                "文件大小超过限制: " + maxFileSize);
        }
    }

    /**
     * 解析 PDF 文件内容
     */
    private String extractPdfContent(File file) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * 解析 DOC 文件内容
     */
    private String extractDocContent(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             HWPFDocument document = new HWPFDocument(fis);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    /**
     * 解析 DOCX 文件内容
     */
    private String extractDocxContent(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {
            StringBuilder content = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                content.append(paragraph.getText()).append("\n");
            }
            return content.toString();
        }
    }

    /**
     * 解析 Markdown 文件内容
     */
    private String extractMarkdownContent(File file) throws IOException {
        String markdownContent = FileUtil.readString(file, StandardCharsets.UTF_8);

        // 使用 CommonMark 解析 Markdown 并转换为纯文本
        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdownContent);
        TextContentRenderer renderer = TextContentRenderer.builder().build();

        return renderer.render(document);
    }

    /**
     * 解析 Excel 文件内容
     */
    private String extractExcelContent(File file, boolean isXls) throws IOException {
        StringBuilder content = new StringBuilder();
        StringBuilder structuredContent = new StringBuilder();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = isXls ? new HSSFWorkbook(fis) : new XSSFWorkbook(fis)) {

            int sheetCount = workbook.getNumberOfSheets();
            content.append("Excel文档包含 ").append(sheetCount).append(" 个工作表\n\n");

            for (int sheetIndex = 0; sheetIndex < sheetCount; sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                String sheetName = sheet.getSheetName();

                content.append("工作表 ").append(sheetIndex + 1).append(": ").append(sheetName).append("\n");
                structuredContent.append("## 工作表: ").append(sheetName).append("\n\n");

                // 分析工作表结构
                analyzeSheetStructure(sheet, content, structuredContent);

                // 提取数据
                extractSheetData(sheet, content, structuredContent);

                content.append("\n");
            }

            log.info("Excel文件解析完成，工作表数量: {}", sheetCount);
            return structuredContent.toString();

        } catch (Exception e) {
            log.error("Excel文件解析失败", e);
            throw new IOException("Excel文件解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 分析工作表结构
     */
    private void analyzeSheetStructure(Sheet sheet, StringBuilder content, StringBuilder structuredContent) {
        int firstRow = sheet.getFirstRowNum();
        int lastRow = sheet.getLastRowNum();
        int totalRows = lastRow - firstRow + 1;

        // 统计列数
        int maxColumns = 0;
        Row headerRow = sheet.getRow(firstRow);
        if (headerRow != null) {
            maxColumns = headerRow.getLastCellNum();
        }

        for (int i = firstRow; i <= lastRow; i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                maxColumns = Math.max(maxColumns, row.getLastCellNum());
            }
        }

        content.append("  - 数据行数: ").append(totalRows).append("\n");
        content.append("  - 列数: ").append(maxColumns).append("\n");

        structuredContent.append("### 基本信息\n");
        structuredContent.append("- 数据行数: ").append(totalRows).append("\n");
        structuredContent.append("- 列数: ").append(maxColumns).append("\n\n");
    }

    /**
     * 提取工作表数据
     */
    private void extractSheetData(Sheet sheet, StringBuilder content, StringBuilder structuredContent) {
        int firstRow = sheet.getFirstRowNum();
        int lastRow = sheet.getLastRowNum();

        if (firstRow < 0 || lastRow < 0) {
            content.append("  - 工作表为空\n");
            structuredContent.append("**工作表为空**\n\n");
            return;
        }

        // 提取表头
        Row headerRow = sheet.getRow(firstRow);
        List<String> headers = new java.util.ArrayList<>();
        if (headerRow != null) {
            for (int cellIndex = 0; cellIndex < headerRow.getLastCellNum(); cellIndex++) {
                Cell cell = headerRow.getCell(cellIndex);
                String cellValue = getCellStringValue(cell);
                headers.add(cellValue.isEmpty() ? "列" + (cellIndex + 1) : cellValue);
            }
        }

        // 显示表头
        if (!headers.isEmpty()) {
            content.append("  - 表头: ").append(String.join(", ", headers)).append("\n");
            structuredContent.append("### 表头\n");
            for (int i = 0; i < headers.size(); i++) {
                structuredContent.append(i + 1).append(". ").append(headers.get(i)).append("\n");
            }
            structuredContent.append("\n");
        }

        // 提取数据样本（前10行）
        int sampleRows = Math.min(10, lastRow - firstRow);
        int dataStartRow = firstRow + 1;

        if (sampleRows > 0) {
            structuredContent.append("### 数据样本（前").append(sampleRows).append("行）\n\n");

            // 创建表格
            structuredContent.append("| ");
            for (String header : headers) {
                structuredContent.append(header).append(" | ");
            }
            structuredContent.append("\n|");
            for (int i = 0; i < headers.size(); i++) {
                structuredContent.append("---|");
            }
            structuredContent.append("\n");

            for (int rowIndex = dataStartRow; rowIndex < dataStartRow + sampleRows; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row != null) {
                    structuredContent.append("| ");
                    for (int cellIndex = 0; cellIndex < headers.size(); cellIndex++) {
                        Cell cell = row.getCell(cellIndex);
                        String cellValue = getCellStringValue(cell);
                        structuredContent.append(cellValue).append(" | ");
                    }
                    structuredContent.append("\n");
                }
            }

            // 如果还有更多数据，添加说明
            if (lastRow - firstRow > 10) {
                structuredContent.append("\n*注：仅显示前10行数据，总计").append(lastRow - firstRow).append("行数据*\n");
            }
            structuredContent.append("\n");
        }

        // 数据分析
        analyzeSheetData(sheet, headers, structuredContent);
    }

    /**
     * 分析工作表数据
     */
    private void analyzeSheetData(Sheet sheet, List<String> headers, StringBuilder structuredContent) {
        int firstRow = sheet.getFirstRowNum() + 1; // 跳过表头
        int lastRow = sheet.getLastRowNum();

        if (firstRow > lastRow) {
            return;
        }

        structuredContent.append("### 数据分析\n\n");

        // 分析每一列的数据类型和特征
        for (int colIndex = 0; colIndex < headers.size(); colIndex++) {
            String header = headers.get(colIndex);
            structuredContent.append("**").append(header).append("**:\n");

            // 统计数据类型
            int stringCount = 0, numberCount = 0, dateCount = 0, emptyCount = 0;
            int totalCells = 0;
            List<String> uniqueValues = new java.util.ArrayList<>();

            for (int rowIndex = firstRow; rowIndex <= lastRow; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row != null) {
                    Cell cell = row.getCell(colIndex);
                    totalCells++;

                    if (cell == null || cell.getCellType() == CellType.BLANK) {
                        emptyCount++;
                        continue;
                    }

                    String cellValue = getCellStringValue(cell);
                    if (cellValue.isEmpty()) {
                        emptyCount++;
                    } else {
                        // 添加到唯一值列表（限制数量）
                        if (uniqueValues.size() < 10) {
                            uniqueValues.add(cellValue);
                        }

                        // 判断数据类型
                        CellType cellType = getEffectiveCellType(cell);
                        switch (cellType) {
                            case NUMERIC:
                                if (DateUtil.isCellDateFormatted(cell)) {
                                    dateCount++;
                                } else {
                                    numberCount++;
                                }
                                break;
                            case STRING:
                                stringCount++;
                                break;
                            default:
                                stringCount++;
                                break;
                        }
                    }
                }
            }

            structuredContent.append("- 数据类型分布: 数值(").append(numberCount).append("), 日期(")
                          .append(dateCount).append("), 文本(").append(stringCount).append("), 空值(")
                          .append(emptyCount).append(")\n");

            // 显示唯一值示例
            if (!uniqueValues.isEmpty()) {
                structuredContent.append("- 示例值: ");
                List<String> sampleValues = uniqueValues.stream().limit(5).toList();
                structuredContent.append(String.join(", ", sampleValues));
                if (uniqueValues.size() > 5) {
                    structuredContent.append(" ...");
                }
                structuredContent.append("\n");
            }

            structuredContent.append("\n");
        }
    }

    /**
     * 获取单元格字符串值
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        CellType cellType = getEffectiveCellType(cell);

        switch (cellType) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numValue = cell.getNumericCellValue();
                    // 如果是整数，不显示小数点
                    if (numValue == (long) numValue) {
                        return String.format("%d", (long) numValue);
                    } else {
                        return String.valueOf(numValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    try {
                        return cell.getStringCellValue();
                    } catch (Exception ex) {
                        return cell.getCellFormula();
                    }
                }
            case BLANK:
            default:
                return "";
        }
    }

    /**
     * 获取有效的单元格类型
     */
    private CellType getEffectiveCellType(Cell cell) {
        if (cell == null) {
            return CellType.BLANK;
        }

        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA) {
            try {
                cell.getCachedFormulaResultType();
            } catch (Exception e) {
                return CellType.STRING;
            }
        }

        return cellType;
    }

    /**
     * 解析文件大小字符串
     */
    private long parseFileSize(String sizeStr) {
        if (StrUtil.isBlank(sizeStr)) {
            return 10 * 1024 * 1024; // 默认 10MB
        }
        
        sizeStr = sizeStr.trim().toLowerCase();
        long multiplier = 1;
        
        if (sizeStr.endsWith("kb")) {
            multiplier = 1024;
            sizeStr = sizeStr.substring(0, sizeStr.length() - 2);
        } else if (sizeStr.endsWith("mb")) {
            multiplier = 1024 * 1024;
            sizeStr = sizeStr.substring(0, sizeStr.length() - 2);
        } else if (sizeStr.endsWith("gb")) {
            multiplier = 1024 * 1024 * 1024;
            sizeStr = sizeStr.substring(0, sizeStr.length() - 2);
        }
        
        try {
            return Long.parseLong(sizeStr.trim()) * multiplier;
        } catch (NumberFormatException e) {
            return 10 * 1024 * 1024; // 默认 10MB
        }
    }

    /**
     * 删除文件
     */
    public void deleteFile(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
            log.info("文件删除成功: {}", filePath);
        } catch (IOException e) {
            log.warn("文件删除失败: {}", filePath, e);
        }
    }
}
