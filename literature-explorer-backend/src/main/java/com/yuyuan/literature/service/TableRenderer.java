package com.yuyuan.literature.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Word表格渲染器
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Slf4j
@Service
public class TableRenderer {

    /**
     * 创建表格
     *
     * @param tableMarkdown 表格Markdown内容
     * @param document Word文档
     */
    public void createTable(String tableMarkdown, XWPFDocument document) {
        log.debug("开始渲染表格，表格行数: {}", tableMarkdown.split("\n").length);

        try {
            // 解析表格行
            List<String> rows = parseTableRows(tableMarkdown);

            if (rows.size() < 2) {
                log.warn("表格内容不足，跳过表格创建");
                return; // 至少需要表头和分隔符
            }

            // 跳过分隔符行（第二行）
            List<String> dataRows = new ArrayList<>();
            dataRows.add(rows.get(0)); // 表头
            for (int i = 2; i < rows.size(); i++) {
                dataRows.add(rows.get(i));
            }

            if (dataRows.size() < 1) {
                log.warn("表格没有有效数据行，跳过表格创建");
                return;
            }

            // 创建表格
            int cols = countColumns(dataRows.get(0));
            XWPFTable table = document.createTable(dataRows.size(), cols);

            // 计算智能列宽
            int[] columnWidths = calculateIntelligentColumnWidths(dataRows, cols);

            // 应用表格样式
            applyTableStyle(table);

            // 填充表格内容
            for (int i = 0; i < dataRows.size(); i++) {
                XWPFTableRow row = table.getRow(i);
                String[] cells = parseTableRow(dataRows.get(i));

                if (i == 0) {
                    // 表头行
                    createHeaderRow(row, cells);
                } else {
                    // 数据行
                    createDataRow(row, cells, i);
                }
            }

            // 应用计算出的列宽
            applyColumnWidths(table, columnWidths);

            log.debug("表格渲染完成，行数: {}，列数: {}", dataRows.size(), cols);

        } catch (Exception e) {
            log.error("表格渲染失败", e);
        }
    }

    /**
     * 解析表格行
     */
    private List<String> parseTableRows(String tableMarkdown) {
        List<String> rows = new ArrayList<>();
        String[] lines = tableMarkdown.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("|") && line.endsWith("|")) {
                rows.add(line);
            }
        }

        return rows;
    }

    /**
     * 计算智能列宽（基于内容长度）
     */
    private int[] calculateIntelligentColumnWidths(List<String> dataRows, int cols) {
        int[] maxWidths = new int[cols]; // 存储每列的最大内容长度
        int[] columnWidths = new int[cols]; // 存储最终列宽数值（以twip为单位）

        // 计算每列的最大内容长度
        for (String row : dataRows) {
            String[] cells = parseTableRow(row);
            for (int i = 0; i < Math.min(cells.length, cols); i++) {
                // 计算单元格内容的显示宽度（考虑中文字符）
                int cellWidth = calculateDisplayWidth(cells[i].trim());
                maxWidths[i] = Math.max(maxWidths[i], cellWidth);
            }
        }

        // 计算总的内容长度
        int totalWidth = 0;
        for (int width : maxWidths) {
            totalWidth += width;
        }

        // 如果总长度为0，使用平均分配
        if (totalWidth == 0) {
            for (int i = 0; i < cols; i++) {
                columnWidths[i] = 2000; // 默认每列2000 twip
            }
            return columnWidths;
        }

        // 计算表格总宽度（以twip为单位，A4页面可用宽度约8500 twip）
        int tableTotalWidth = 6000; // 60%的A4页面宽度

        // 设置最小和最大列宽限制
        int minColumnWidth = 800;  // 最小列宽（约1.1cm）
        int maxColumnWidth = 4000; // 最大列宽（约5.6cm）

        // 根据内容长度比例分配宽度
        for (int i = 0; i < cols; i++) {
            if (totalWidth > 0) {
                // 基于内容长度的比例
                double ratio = (double) maxWidths[i] / totalWidth;
                int calculatedWidth = (int) (tableTotalWidth * ratio);

                // 应用最小和最大宽度限制
                columnWidths[i] = Math.max(minColumnWidth, Math.min(maxColumnWidth, calculatedWidth));
            } else {
                columnWidths[i] = minColumnWidth;
            }
        }

        // 调整总和以确保不超过表格总宽度
        int sumWidths = 0;
        for (int width : columnWidths) {
            sumWidths += width;
        }

        if (sumWidths > tableTotalWidth) {
            // 按比例缩减以适应表格总宽度
            double scale = (double) tableTotalWidth / sumWidths;
            for (int i = 0; i < cols; i++) {
                columnWidths[i] = (int) (columnWidths[i] * scale);
                // 确保不小于最小宽度
                columnWidths[i] = Math.max(minColumnWidth, columnWidths[i]);
            }
        }

        log.debug("计算列宽完成: {}", java.util.Arrays.toString(columnWidths));
        return columnWidths;
    }

    /**
     * 计算文本的显示宽度（考虑中文字符）
     */
    private int calculateDisplayWidth(String text) {
        if (text == null || text.isEmpty()) {
            return 50; // 空文本的最小宽度
        }

        int width = 0;
        for (char c : text.toCharArray()) {
            // 中文字符和全角字符按2个字符宽度计算
            if (isFullWidthCharacter(c)) {
                width += 2;
            } else {
                width += 1;
            }
        }

        // 添加一些缓冲空间
        return width + 20;
    }

    /**
     * 判断字符是否为全角字符（包括中文）
     */
    private boolean isFullWidthCharacter(char c) {
        // 中文字符范围
        if (c >= 0x4E00 && c <= 0x9FFF) {
            return true;
        }
        // 全角符号范围
        if (c >= 0xFF00 && c <= 0xFFEF) {
            return true;
        }
        // 其他全角字符
        if ((c >= 0x3000 && c <= 0x303F) || // CJK符号和标点
            (c >= 0xFF00 && c <= 0xFFEF)) {   // 半角及全角形式
            return true;
        }
        return false;
    }

    /**
     * 应用列宽到表格
     */
    private void applyColumnWidths(XWPFTable table, int[] columnWidths) {
        try {
            for (int col = 0; col < columnWidths.length; col++) {
                for (int row = 0; row < table.getNumberOfRows(); row++) {
                    XWPFTableRow tableRow = table.getRow(row);
                    if (tableRow.getCell(col) != null) {
                        XWPFTableCell cell = tableRow.getCell(col);

                        // 设置单元格宽度
                        CTTcPr tcPr = cell.getCTTc().getTcPr();
                        if (tcPr == null) {
                            tcPr = cell.getCTTc().addNewTcPr();
                        }

                        CTTblWidth cellWidth = tcPr.getTcW();
                        if (cellWidth == null) {
                            cellWidth = tcPr.addNewTcW();
                        }

                        cellWidth.setType(STTblWidth.DXA); // 固定宽度单位
                        cellWidth.setW(BigInteger.valueOf(columnWidths[col]));
                    }
                }
            }
            log.debug("列宽应用完成: {}", java.util.Arrays.toString(columnWidths));
        } catch (Exception e) {
            log.error("应用列宽失败", e);
        }
    }

    /**
     * 解析表格行内容
     */
    private String[] parseTableRow(String row) {
        // 移除首尾的|，然后按|分割
        String content = row.substring(1, row.length() - 1);
        return content.split("\\|");
    }

    /**
     * 计算列数
     */
    private int countColumns(String headerRow) {
        return parseTableRow(headerRow).length;
    }

    /**
     * 应用表格样式
     */
    private void applyTableStyle(XWPFTable table) {
        try {
            // 设置表格居中
            CTTblPr tblPr = table.getCTTbl().getTblPr();
            if (tblPr == null) {
                tblPr = table.getCTTbl().addNewTblPr();
            }

            // 设置表格宽度为90%以避免超出边界
            CTTblWidth tblWidth = tblPr.addNewTblW();
            tblWidth.setType(STTblWidth.PCT);
            tblWidth.setW(BigInteger.valueOf(5000)); // 90%

            // 设置表格边框
            CTTblBorders borders = tblPr.addNewTblBorders();

            // 设置边框样式
            CTBorder topBorder = borders.addNewTop();
            topBorder.setVal(STBorder.SINGLE);
            topBorder.setSz(BigInteger.valueOf(4));

            CTBorder leftBorder = borders.addNewLeft();
            leftBorder.setVal(STBorder.SINGLE);
            leftBorder.setSz(BigInteger.valueOf(4));

            CTBorder bottomBorder = borders.addNewBottom();
            bottomBorder.setVal(STBorder.SINGLE);
            bottomBorder.setSz(BigInteger.valueOf(4));

            CTBorder rightBorder = borders.addNewRight();
            rightBorder.setVal(STBorder.SINGLE);
            rightBorder.setSz(BigInteger.valueOf(4));

            CTBorder insideHBorder = borders.addNewInsideH();
            insideHBorder.setVal(STBorder.SINGLE);
            insideHBorder.setSz(BigInteger.valueOf(4));

            CTBorder insideVBorder = borders.addNewInsideV();
            insideVBorder.setVal(STBorder.SINGLE);
            insideVBorder.setSz(BigInteger.valueOf(4));

        } catch (Exception e) {
            log.error("应用表格样式失败", e);
        }
    }

    /**
     * 创建表头行
     */
    private void createHeaderRow(XWPFTableRow row, String[] cells) {
        for (int i = 0; i < cells.length; i++) {
            XWPFTableCell cell = i < row.getTableCells().size() ? row.getCell(i) : row.createCell();

            // 设置单元格内容
            String cellText = cells[i].trim();
            XWPFParagraph para = cell.getParagraphs().get(0);

            // 清除现有的runs
            for (int j = para.getRuns().size() - 1; j >= 0; j--) {
                para.removeRun(j);
            }

            // 添加包含粗体处理的文本
            parseAndRenderBoldTextInCell(para, cellText, true);

            // 设置单元格样式
            cell.setColor("4F81BD"); // 蓝色背景
            cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);

            // 设置段落居中
            para.setAlignment(ParagraphAlignment.CENTER);

            // 设置单元格边距
            CTTcPr tcPr = cell.getCTTc().getTcPr();
            if (tcPr == null) {
                tcPr = cell.getCTTc().addNewTcPr();
            }

            CTTcMar cellMar = tcPr.addNewTcMar();
            cellMar.addNewLeft().setW(BigInteger.valueOf(72)); // 0.05 inch
            cellMar.addNewRight().setW(BigInteger.valueOf(72));
            cellMar.addNewTop().setW(BigInteger.valueOf(72));
            cellMar.addNewBottom().setW(BigInteger.valueOf(72));
        }
    }

    /**
     * 创建数据行
     */
    private void createDataRow(XWPFTableRow row, String[] cells, int rowIndex) {
        for (int i = 0; i < cells.length; i++) {
            XWPFTableCell cell = i < row.getTableCells().size() ? row.getCell(i) : row.createCell();

            // 设置单元格内容
            String cellText = cells[i].trim();
            XWPFParagraph para = cell.getParagraphs().get(0);

            // 清除现有的runs
            for (int j = para.getRuns().size() - 1; j >= 0; j--) {
                para.removeRun(j);
            }

            // 添加包含粗体处理的文本
            parseAndRenderBoldTextInCell(para, cellText, false);

            // 设置单元格样式 - 交替行背景色
            if (rowIndex % 2 == 0) {
                cell.setColor("F2F2F2"); // 浅灰色背景
            } else {
                cell.setColor("FFFFFF"); // 白色背景
            }

            cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);

            // 设置段落居左
            para.setAlignment(ParagraphAlignment.LEFT);

            // 设置单元格边距
            CTTcPr tcPr = cell.getCTTc().getTcPr();
            if (tcPr == null) {
                tcPr = cell.getCTTc().addNewTcPr();
            }

            CTTcMar cellMar = tcPr.addNewTcMar();
            cellMar.addNewLeft().setW(BigInteger.valueOf(72)); // 0.05 inch
            cellMar.addNewRight().setW(BigInteger.valueOf(72));
            cellMar.addNewTop().setW(BigInteger.valueOf(72));
            cellMar.addNewBottom().setW(BigInteger.valueOf(72));
        }
    }

    /**
     * 自动调整列宽（保持90%宽度限制）
     */
    private void autoSizeColumns(XWPFTable table) {
        try {
            // 设置表格为固定90%宽度，避免自动调整超出边界
            CTTblPr tblPr = table.getCTTbl().getTblPr();
            if (tblPr == null) {
                tblPr = table.getCTTbl().addNewTblPr();
            }

            CTTblWidth tblWidth = tblPr.getTblW();
            if (tblWidth == null) {
                tblWidth = tblPr.addNewTblW();
            }

            // 保持90%宽度，不使用AUTO
            tblWidth.setType(STTblWidth.AUTO);
//            tblWidth.setW(BigInteger.valueOf(5000)); // 90%

            // 设置表格居中
            if (!tblPr.isSetJc()) {
                tblPr.addNewJc();
            }
            // 表格默认居中对齐

            // 设置表格无缩进
            if (tblPr.isSetTblInd()) {
                tblPr.unsetTblInd();
            }

            // 设置表格布局为固定，防止内容撑大表格
            CTTblLayoutType tblLayout = tblPr.getTblLayout();
            if (tblLayout == null) {
                tblLayout = tblPr.addNewTblLayout();
            }
            tblLayout.setType(STTblLayoutType.FIXED);

        } catch (Exception e) {
            log.error("自动调整列宽失败", e);
        }
    }

    /**
     * 解析并渲染表格单元格中包含粗体语法的文本
     */
    private void parseAndRenderBoldTextInCell(XWPFParagraph paragraph, String text, boolean isHeader) {
        // 粗体正则表达式：匹配 **文本**
        Pattern boldPattern = Pattern.compile("\\*\\*(.*?)\\*\\*");
        Matcher matcher = boldPattern.matcher(text);

        int lastIndex = 0;
        while (matcher.find()) {
            // 添加粗体前的普通文本
            String normalText = text.substring(lastIndex, matcher.start());
            if (!normalText.isEmpty()) {
                XWPFRun normalRun = paragraph.createRun();
                normalRun.setText(normalText);
                normalRun.setFontFamily("微软雅黑");
                normalRun.setFontSize(isHeader ? 11 : 10);
                if (isHeader) {
                    normalRun.setBold(true);
                    normalRun.setColor("FFFFFF"); // 表头白色文字
                } else {
                    normalRun.setColor("333333"); // 数据行黑色文字
                }
            }

            // 添加粗体文本
            String boldText = matcher.group(1);
            XWPFRun boldRun = paragraph.createRun();
            boldRun.setText(boldText);
            boldRun.setFontFamily("微软雅黑");
            boldRun.setFontSize(isHeader ? 11 : 10);
            boldRun.setBold(true);
            if (isHeader) {
                boldRun.setColor("FFFFFF"); // 表头白色文字
            } else {
                boldRun.setColor("000000"); // 数据行黑色粗体文字
            }

            lastIndex = matcher.end();
        }

        // 添加剩余的普通文本
        if (lastIndex < text.length()) {
            String remainingText = text.substring(lastIndex);
            XWPFRun remainingRun = paragraph.createRun();
            remainingRun.setText(remainingText);
            remainingRun.setFontFamily("微软雅黑");
            remainingRun.setFontSize(isHeader ? 11 : 10);
            if (isHeader) {
                remainingRun.setBold(true);
                remainingRun.setColor("FFFFFF"); // 表头白色文字
            } else {
                remainingRun.setColor("333333"); // 数据行黑色文字
            }
        }
    }
}