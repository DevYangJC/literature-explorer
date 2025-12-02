package com.yuyuan.literature.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 分类响应 DTO
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Data
public class ClassificationResponse {

    /**
     * 分类标签
     */
    private List<String> tags;

    /**
     * 描述
     */
    private String desc;
}
