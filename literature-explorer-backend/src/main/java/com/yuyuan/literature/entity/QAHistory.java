package com.yuyuan.literature.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 问答历史记录实体类
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "qa_history", autoResultMap = true)
@Schema(description = "问答历史记录")
public class QAHistory {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /**
     * 会话ID（同一次对话的多轮问答共享）
     */
    @TableField("session_id")
    @Schema(description = "会话ID")
    private String sessionId;

    /**
     * 关联的文献ID（单文献问答时有值）
     */
    @TableField("literature_id")
    @Schema(description = "文献ID")
    private Long literatureId;

    /**
     * 问题内容
     */
    @TableField("question")
    @Schema(description = "问题内容")
    private String question;

    /**
     * 答案内容
     */
    @TableField("answer")
    @Schema(description = "答案内容")
    private String answer;

    /**
     * 是否跨文献检索
     */
    @TableField("cross_doc")
    @Schema(description = "是否跨文献检索")
    private Boolean crossDoc;

    /**
     * 检索关键词（跨文献时有值）
     */
    @TableField("keyword")
    @Schema(description = "检索关键词")
    private String keyword;

    /**
     * 会话标题（会话的第一个问题作为标题）
     */
    @TableField("session_title")
    @Schema(description = "会话标题")
    private String sessionTitle;

    /**
     * 问答序号（在会话中的顺序，从1开始）
     */
    @TableField("sequence")
    @Schema(description = "问答序号")
    private Integer sequence;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    /**
     * 删除标记：0-正常，1-删除
     */
    @TableField("deleted")
    @TableLogic
    @Schema(description = "删除标记")
    private Integer deleted;
}
