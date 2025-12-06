package com.yuyuan.literature.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown转Word转换器
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Slf4j
@Service
public class MarkdownToWordConverter {

    @Autowired
    private WordStyleTemplate styleTemplate;

    @Autowired
    private TableRenderer tableRenderer;

    @Autowired
    private CodeBlockRenderer codeBlockRenderer;

    // Markdown模式匹配
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$");
    private static final Pattern TABLE_PATTERN = Pattern.compile("^\\|(.+)\\|$");
    private static final Pattern TABLE_SEPARATOR_PATTERN = Pattern.compile("^\\|[-\\s|:]+\\|$");
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("^```(\\w+)?\\s*$");
    private static final Pattern LIST_PATTERN = Pattern.compile("^\\s*([\\*\\+\\-]|\\d+\\.)\\s+(.+)$");
    private static final Pattern QUOTE_PATTERN = Pattern.compile("^>\\s+(.+)$");
    private static final Pattern EMPTY_LINE_PATTERN = Pattern.compile("^\\s*$");

    /**
     * 将Markdown内容转换为Word文档
     *
     * @param markdownContent Markdown内容
     * @return Word文档
     */
    public XWPFDocument convertToWord(String markdownContent) {
        log.info("开始转换Markdown到Word文档，内容长度: {}", markdownContent.length());

        XWPFDocument document = new XWPFDocument();

        // 1. 应用样式模板
        styleTemplate.applyStyles(document);

        // 2. 预处理内容
        String[] lines = markdownContent.split("\n");

        // 3. 逐行解析和转换
        int i = 0;
        while (i < lines.length) {
            String line = lines[i].trim();

            if (EMPTY_LINE_PATTERN.matcher(line).matches()) {
                // 空行 - 添加空段落
                document.createParagraph();
                i++;
                continue;
            }

            // 检查代码块
            if (CODE_BLOCK_PATTERN.matcher(line).matches()) {
                i = processCodeBlock(lines, i, document);
                continue;
            }

            // 检查表格
            if (TABLE_PATTERN.matcher(line).matches() && i + 1 < lines.length &&
                TABLE_SEPARATOR_PATTERN.matcher(lines[i + 1].trim()).matches()) {
                i = processTable(lines, i, document);
                continue;
            }

            // 检查标题
            Matcher headingMatcher = HEADING_PATTERN.matcher(line);
            if (headingMatcher.matches()) {
                String hashes = headingMatcher.group(1);
                String text = headingMatcher.group(2);
                createHeading(document, text, hashes.length());
                i++;
                continue;
            }

            // 检查列表
            Matcher listMatcher = LIST_PATTERN.matcher(line);
            if (listMatcher.matches()) {
                i = processList(lines, i, document);
                continue;
            }

            // 检查引用
            Matcher quoteMatcher = QUOTE_PATTERN.matcher(line);
            if (quoteMatcher.matches()) {
                String text = quoteMatcher.group(1);
                createQuote(document, text);
                i++;
                continue;
            }

            // 普通段落
            createParagraph(document, line);
            i++;
        }

        log.info("Markdown到Word转换完成");
        return document;
    }

    /**
     * 处理代码块
     */
    private int processCodeBlock(String[] lines, int startIndex, XWPFDocument document) {
        String language = "";
        String firstLine = lines[startIndex].trim();
        Matcher codeMatcher = CODE_BLOCK_PATTERN.matcher(firstLine);

        if (codeMatcher.matches()) {
            language = codeMatcher.group(1) != null ? codeMatcher.group(1) : "";
        }

        StringBuilder codeContent = new StringBuilder();
        int i = startIndex + 1;

        // 查找代码块结束标记
        while (i < lines.length) {
            String line = lines[i];
            if (CODE_BLOCK_PATTERN.matcher(line.trim()).matches()) {
                break;
            }
            codeContent.append(line).append("\n");
            i++;
        }

        // 使用代码块渲染器创建代码块
        codeBlockRenderer.createCodeBlock(document, codeContent.toString().trim(), language);

        return i + 1; // 跳过结束标记
    }

    /**
     * 处理表格
     */
    private int processTable(String[] lines, int startIndex, XWPFDocument document) {
        StringBuilder tableContent = new StringBuilder();
        int i = startIndex;

        // 收集表格内容
        while (i < lines.length) {
            String line = lines[i].trim();
            if (TABLE_PATTERN.matcher(line).matches()) {
                tableContent.append(line).append("\n");
                i++;
            } else {
                break;
            }
        }

        // 使用表格渲染器创建表格
        tableRenderer.createTable(tableContent.toString(), document);

        return i;
    }

    /**
     * 处理列表
     */
    private int processList(String[] lines, int startIndex, XWPFDocument document) {
        // 创建列表
        XWPFParagraph listPara = document.createParagraph();
        XWPFNumbering numbering = document.getNumbering();

        // 简化处理：创建项目符号列表
        int i = startIndex;
        while (i < lines.length) {
            String line = lines[i].trim();
            Matcher listMatcher = LIST_PATTERN.matcher(line);

            if (listMatcher.matches()) {
                String marker = listMatcher.group(1);
                String text = listMatcher.group(2);

                XWPFParagraph para = document.createParagraph();
                para.setNumID(BigInteger.valueOf(1)); // 使用项目符号编号

                XWPFRun run = para.createRun();
                run.setText(text);
                run.setFontFamily("微软雅黑");
                run.setFontSize(11);

                i++;
            } else if (EMPTY_LINE_PATTERN.matcher(line).matches()) {
                i++;
            } else {
                break;
            }
        }

        return i;
    }

    /**
     * 创建标题
     */
    private void createHeading(XWPFDocument document, String text, int level) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setStyle("Heading" + level);

        // 处理标题中的粗体语法
        parseAndRenderBoldText(paragraph, text);

        // 设置所有Run为标题样式
        for (XWPFRun run : paragraph.getRuns()) {
            run.setFontFamily("微软雅黑");
            run.setBold(true);

            // 根据标题级别设置字体大小和颜色
            switch (level) {
                case 1:
                    run.setFontSize(16);
                    run.setColor("000000");
                    break;
                case 2:
                    run.setFontSize(14);
                    run.setColor("000000");
                    break;
                case 3:
                    run.setFontSize(12);
                    run.setColor("000000");
                    break;
                default:
                    run.setFontSize(12);
                    break;
            }
        }

        paragraph.setSpacingAfter(200);
    }

    /**
     * 创建段落
     */
    private void createParagraph(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.BOTH); // 两端对齐

        // 处理Markdown粗体语法 **文本**
        parseAndRenderBoldText(paragraph, text);

        paragraph.setSpacingAfter(100);
    }

    /**
     * 解析并渲染包含粗体语法的文本
     */
    private void parseAndRenderBoldText(XWPFParagraph paragraph, String text) {
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
                normalRun.setFontSize(11);
                normalRun.setColor("333333");
            }

            // 添加粗体文本
            String boldText = matcher.group(1);
            XWPFRun boldRun = paragraph.createRun();
            boldRun.setText(boldText);
            boldRun.setFontFamily("微软雅黑");
            boldRun.setFontSize(11);
            boldRun.setBold(true);
            boldRun.setColor("000000");

            lastIndex = matcher.end();
        }

        // 添加剩余的普通文本
        if (lastIndex < text.length()) {
            String remainingText = text.substring(lastIndex);
            XWPFRun remainingRun = paragraph.createRun();
            remainingRun.setText(remainingText);
            remainingRun.setFontFamily("微软雅黑");
            remainingRun.setFontSize(11);
            remainingRun.setColor("333333");
        }
    }

    /**
     * 创建引用
     */
    private void createQuote(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.LEFT);
        paragraph.setIndentationLeft(720); // 左缩进

        // 设置引用背景色
        paragraph.getCTP().getPPr().addNewShd().setFill("F8F8F8");

        // 处理引用中的粗体语法
        parseAndRenderBoldText(paragraph, text);

        // 设置所有Run为引用样式
        for (XWPFRun run : paragraph.getRuns()) {
            run.setFontFamily("宋体");
            run.setFontSize(11);
            run.setColor("666666");
            run.setItalic(true);
        }

        paragraph.setSpacingAfter(100);
    }
}