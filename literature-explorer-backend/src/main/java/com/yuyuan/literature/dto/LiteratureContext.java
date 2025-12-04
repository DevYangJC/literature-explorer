package com.yuyuan.literature.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文献上下文 DTO（用于问答系统）
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiteratureContext {

    /**
     * 文献ID
     */
    private Long literatureId;

    /**
     * 文献标题
     */
    private String title;

    /**
     * 文献描述
     */
    private String description;

    /**
     * 阅读指南内容
     */
    private String readingGuide;

    /**
     * 标签
     */
    private String tags;

    /**
     * 相关性分数（用于排序）
     */
    private Double relevanceScore;
}
