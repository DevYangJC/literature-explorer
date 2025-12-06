package com.yuyuan.literature.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.stereotype.Service;

/**
 * Word样式模板类（简化版）
 * 用于设置文档基础样式
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Slf4j
@Service
public class WordStyleTemplate {

    /**
     * 应用样式模板到Word文档
     *
     * @param document Word文档
     */
    public void applyStyles(XWPFDocument document) {
        log.debug("开始应用Word样式模板");

        try {
            // 设置页面布局
            setupPageLayout(document);

            log.debug("Word样式模板应用完成");

        } catch (Exception e) {
            log.error("应用Word样式模板失败", e);
        }
    }

    /**
     * 设置页面布局
     */
    private void setupPageLayout(XWPFDocument document) {
        try {
            // 获取文档主体
            CTSectPr sectionPr = document.getDocument().getBody().addNewSectPr();

            // 设置页边距（单位：twips, 1英寸=1440twips）
            CTPageMar pageMar = sectionPr.addNewPgMar();
            pageMar.setLeft(java.math.BigInteger.valueOf(1440));  // 1英寸
            pageMar.setRight(java.math.BigInteger.valueOf(1440)); // 1英寸
            pageMar.setTop(java.math.BigInteger.valueOf(1440));   // 1英寸
            pageMar.setBottom(java.math.BigInteger.valueOf(1440));// 1英寸

            // 设置页面大小（A4）
            CTPageSz pgSz = sectionPr.addNewPgSz();
            pgSz.setW(java.math.BigInteger.valueOf(11907)); // A4宽度
            pgSz.setH(java.math.BigInteger.valueOf(16839)); // A4高度
            pgSz.setOrient(STPageOrientation.PORTRAIT);

        } catch (Exception e) {
            log.error("设置页面布局失败", e);
        }
    }
}