package com.yuyuan.literature.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 文献分析请求 DTO
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Data
@Schema(description = "文献分析请求")
public class LiteratureAnalysisRequest {

    /**
     * API Key
     */
    @Schema(description = "Kimi AI API Key", required = true)
    @NotBlank(message = "API Key 不能为空")
    private String apiKey;
}
