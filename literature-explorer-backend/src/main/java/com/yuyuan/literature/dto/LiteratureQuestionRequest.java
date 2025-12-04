package com.yuyuan.literature.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 文献问答请求 DTO
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Data
@Schema(description = "文献问答请求")
public class LiteratureQuestionRequest {

    /**
     * 用户问题
     */
    @NotBlank(message = "问题不能为空")
    @Schema(description = "用户问题", example = "这篇文献的核心创新点是什么？")
    private String question;

    /**
     * Kimi API Key
     */
    @NotBlank(message = "API Key 不能为空")
    @Schema(description = "Kimi API Key")
    private String apiKey;

    /**
     * 文献ID（单文献问答时必传）
     */
    @Schema(description = "文献ID（单文献问答时传）")
    private Long literatureId;

    /**
     * 是否跨文献检索
     */
    @Schema(description = "是否跨文献检索", defaultValue = "false")
    private Boolean crossDoc = false;

    /**
     * 检索返回的文献数量（跨文献时有效）
     */
    @Schema(description = "检索返回的文献数量", defaultValue = "5")
    private Integer topK = 5;

    /**
     * 关键词（用于跨文献检索，可选）
     */
    @Schema(description = "关键词（用于跨文献检索，可选）")
    private String keyword;
}
