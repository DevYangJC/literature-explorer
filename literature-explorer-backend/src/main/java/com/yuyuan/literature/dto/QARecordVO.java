package com.yuyuan.literature.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问答记录视图对象
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Data
@Schema(description = "问答记录")
public class QARecordVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "问题")
    private String question;

    @Schema(description = "答案")
    private String answer;

    @Schema(description = "序号")
    private Integer sequence;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
