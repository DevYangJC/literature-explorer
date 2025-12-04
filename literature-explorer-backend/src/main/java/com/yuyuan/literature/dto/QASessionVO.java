package com.yuyuan.literature.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 问答会话视图对象
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Data
@Schema(description = "问答会话")
public class QASessionVO {

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "会话标题")
    private String sessionTitle;

    @Schema(description = "关联文献ID")
    private Long literatureId;

    @Schema(description = "关联文献名称")
    private String literatureName;

    @Schema(description = "问答数量")
    private Integer qaCount;

    @Schema(description = "是否跨文献")
    private Boolean crossDoc;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    @Schema(description = "问答记录列表")
    private List<QARecordVO> records;
}
