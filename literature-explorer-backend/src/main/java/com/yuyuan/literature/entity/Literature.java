package com.yuyuan.literature.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文献实体类
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "literature", autoResultMap = true)
@Schema(description = "文献实体")
public class Literature {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /**
     * 原始文件名
     */
    @TableField("original_name")
    @Schema(description = "原始文件名")
    private String originalName;

    /**
     * 文件存储路径
     */
    @TableField("file_path")
    @Schema(description = "文件存储路径")
    private String filePath;

    /**
     * 文件大小（字节）
     */
    @TableField("file_size")
    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    /**
     * 文件类型
     */
    @TableField("file_type")
    @Schema(description = "文件类型")
    private String fileType;

    /**
     * 文档内容字符数
     */
    @TableField("content_length")
    @Schema(description = "文档内容字符数")
    private Integer contentLength;

    /**
     * 分类标签（JSON数组）
     */
    @TableField(value = "tags", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "分类标签")
    private List<String> tags;

    /**
     * 文献描述
     */
    @TableField("description")
    @Schema(description = "文献描述")
    private String description;

    /**
     * 阅读指南内容
     */
    @TableField("reading_guide")
    @Schema(description = "阅读指南内容")
    private String readingGuide;

    /**
     * 状态：0-处理中，1-已完成，2-处理失败
     */
    @TableField("status")
    @Schema(description = "状态：0-处理中，1-已完成，2-处理失败")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0-正常，1-已删除
     */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除：0-正常，1-已删除")
    private Integer deleted;

    /**
     * 状态枚举
     */
    @Getter
    public enum Status {
        PROCESSING(0, "处理中"),
        COMPLETED(1, "已完成"),
        FAILED(2, "处理失败");

        private final Integer code;
        private final String desc;

        Status(Integer code, String desc) {
            this.code = code;
            this.desc = desc;
        }

    }
}
