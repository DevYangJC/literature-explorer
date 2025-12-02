package com.yuyuan.literature.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文献展示 VO
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Data
@Schema(description = "文献展示对象")
public class LiteratureVO {

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    private Long id;

    /**
     * 原始文件名
     */
    @Schema(description = "原始文件名")
    private String originalName;

    /**
     * 文件大小（字节）
     */
    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    /**
     * 文件类型
     */
    @Schema(description = "文件类型")
    private String fileType;

    /**
     * 文档内容字符数
     */
    @Schema(description = "文档内容字符数")
    private Integer contentLength;

    /**
     * 分类标签
     */
    @Schema(description = "分类标签")
    private List<String> tags;

    /**
     * 文献描述
     */
    @Schema(description = "文献描述")
    private String description;

    /**
     * 阅读指南内容（摘要，不返回完整内容）
     */
    @Schema(description = "阅读指南摘要")
    private String readingGuideSummary;

    /**
     * 状态：0-处理中，1-已完成，2-处理失败
     */
    @Schema(description = "状态：0-处理中，1-已完成，2-处理失败")
    private Integer status;

    /**
     * 状态描述
     */
    @Schema(description = "状态描述")
    private String statusDesc;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
