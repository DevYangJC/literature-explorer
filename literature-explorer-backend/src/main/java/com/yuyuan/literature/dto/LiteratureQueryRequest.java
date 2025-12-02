package com.yuyuan.literature.dto;

import com.yuyuan.literature.common.request.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 文献查询请求 DTO
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "文献查询请求")
public class LiteratureQueryRequest extends PageRequest {

    /**
     * 关键词搜索（搜索文件名、描述、阅读指南）
     */
    @Schema(description = "关键词搜索", example = "人工智能")
    private String keyword;

    /**
     * 分类标签过滤
     */
    @Schema(description = "分类标签过滤", example = "[\"人工智能\", \"机器学习\"]")
    private List<String> tags;

    /**
     * 文件类型过滤
     */
    @Schema(description = "文件类型过滤", example = "pdf", allowableValues = {"pdf", "doc", "docx", "md", "markdown"})
    private String fileType;

    /**
     * 状态过滤
     */
    @Schema(description = "状态过滤", example = "1", allowableValues = {"0", "1", "2"})
    private Integer status;

    /**
     * 开始时间（创建时间范围查询）
     */
    @Schema(description = "开始时间", example = "2024-01-01")
    private String startDate;

    /**
     * 结束时间（创建时间范围查询）
     */
    @Schema(description = "结束时间", example = "2024-12-31")
    private String endDate;
}
