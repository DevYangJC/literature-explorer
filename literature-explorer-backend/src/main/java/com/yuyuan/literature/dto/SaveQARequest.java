package com.yuyuan.literature.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 保存问答请求
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Data
@Schema(description = "保存问答请求")
public class SaveQARequest {

    @Schema(description = "会话ID（新会话留空，继续对话时传入）")
    private String sessionId;

    @NotBlank(message = "问题不能为空")
    @Schema(description = "问题", required = true)
    private String question;

    @NotBlank(message = "答案不能为空")
    @Schema(description = "答案", required = true)
    private String answer;

    @Schema(description = "文献ID（单文献问答时传）")
    private Long literatureId;

    @Schema(description = "是否跨文献检索")
    private Boolean crossDoc = false;

    @Schema(description = "检索关键词")
    private String keyword;

    @Schema(description = "会话标题（新会话时可传，默认使用第一个问题）")
    private String sessionTitle;
}
