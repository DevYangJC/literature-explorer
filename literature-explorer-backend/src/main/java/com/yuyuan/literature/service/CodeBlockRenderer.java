package com.yuyuan.literature.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.stereotype.Service;

import java.math.BigInteger;

/**
 * Word代码块渲染器
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Slf4j
@Service
public class CodeBlockRenderer {

    /**
     * 创建代码块
     *
     * @param document Word文档
     * @param codeContent 代码内容
     * @param language 编程语言
     */
    public void createCodeBlock(XWPFDocument document, String codeContent, String language) {
        log.debug("开始渲染代码块，语言: {}, 行数: {}", language, codeContent.split("\n").length);

        try {
            // 创建语言标签段落
            if (language != null && !language.trim().isEmpty()) {
                createLanguageLabel(document, language);
            }

            // 创建代码块容器段落（用于背景色）
            XWPFParagraph containerPara = document.createParagraph();
            containerPara.setSpacingBefore(100);
            containerPara.setSpacingAfter(100);

            XWPFRun containerRun = containerPara.createRun();
            containerRun.setText("代码块:");
            containerRun.setFontFamily("微软雅黑");
            containerRun.setFontSize(10);
            containerRun.setBold(true);
            containerRun.setColor("666666");

            // 按行处理代码内容
            String[] lines = codeContent.split("\n");
            for (String line : lines) {
                createCodeLine(document, line);
            }

            log.debug("代码块渲染完成");

        } catch (Exception e) {
            log.error("代码块渲染失败", e);
        }
    }

    /**
     * 创建语言标签
     */
    private void createLanguageLabel(XWPFDocument document, String language) {
        XWPFParagraph labelPara = document.createParagraph();
        labelPara.setSpacingBefore(100);
        labelPara.setSpacingAfter(50);

        XWPFRun labelRun = labelPara.createRun();
        labelRun.setText("语言: " + language);
        labelRun.setFontFamily("微软雅黑");
        labelRun.setFontSize(9);
        labelRun.setColor("999999");
        labelRun.setItalic(true);

        // 设置缩进
        labelPara.setIndentationLeft(360);
    }

    /**
     * 创建代码行
     */
    private void createCodeLine(XWPFDocument document, String line) {
        XWPFParagraph codePara = document.createParagraph();
        codePara.setAlignment(ParagraphAlignment.LEFT);
        codePara.setSpacingAfter(20);

        // 设置左缩进
        codePara.setIndentationLeft(360);
        codePara.setIndentationRight(180);

        // 创建代码内容运行
        XWPFRun codeRun = codePara.createRun();
        codeRun.setText(line);
        codeRun.setFontFamily("Consolas, Monaco, monospace");
        codeRun.setFontSize(10);
        codeRun.setColor("333333");

        // 设置等宽字体特性
        codeRun.setBold(false);
        codeRun.setItalic(false);
    }

    /**
     * 创建带行号的代码块（简化版）
     */
    public void createCodeBlockWithLineNumbers(XWPFDocument document, String codeContent, String language) {
        log.debug("开始渲染带行号的代码块，语言: {}, 行数: {}", language, codeContent.split("\n").length);

        try {
            // 创建语言标签
            if (language != null && !language.trim().isEmpty()) {
                createLanguageLabel(document, language);
            }

            // 创建标题行
            XWPFParagraph headerPara = document.createParagraph();
            XWPFRun headerRun = headerPara.createRun();
            headerRun.setText("代码:");
            headerRun.setFontFamily("微软雅黑");
            headerRun.setFontSize(10);
            headerRun.setBold(true);

            // 按行处理代码内容，每行前面加行号
            String[] lines = codeContent.split("\n");
            for (int i = 0; i < lines.length; i++) {
                createCodeLineWithNumber(document, lines[i], i + 1);
            }

            log.debug("带行号的代码块渲染完成");

        } catch (Exception e) {
            log.error("带行号的代码块渲染失败", e);
        }
    }

    /**
     * 创建带行号的代码行
     */
    private void createCodeLineWithNumber(XWPFDocument document, String line, int lineNumber) {
        XWPFParagraph codePara = document.createParagraph();
        codePara.setAlignment(ParagraphAlignment.LEFT);
        codePara.setSpacingAfter(20);

        // 设置左缩进
        codePara.setIndentationLeft(360);
        codePara.setIndentationRight(180);

        // 行号运行
        XWPFRun numberRun = codePara.createRun();
        numberRun.setText(String.format("%3d: ", lineNumber));
        numberRun.setFontFamily("Consolas, Monaco, monospace");
        numberRun.setFontSize(9);
        numberRun.setColor("999999");

        // 代码内容运行
        XWPFRun codeRun = codePara.createRun();
        codeRun.setText(line);
        codeRun.setFontFamily("Consolas, Monaco, monospace");
        codeRun.setFontSize(10);
        codeRun.setColor("333333");

        // 设置等宽字体特性
        codeRun.setBold(false);
        codeRun.setItalic(false);
    }
}