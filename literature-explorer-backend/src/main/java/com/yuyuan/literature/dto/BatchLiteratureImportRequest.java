package com.yuyuan.literature.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 批量文献导入请求
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Data
@Schema(description = "批量文献导入请求")
public class BatchLiteratureImportRequest {

    @NotEmpty(message = "文件列表不能为空")
    @Size(max = 16, message = "批量导入一次最多16个文件")
    @Schema(description = "文献文件列表", required = true)
    private List<MultipartFile> files;

    @NotBlank(message = "API Key 不能为空")
    @Schema(description = "Kimi AI API Key", required = true)
    private String apiKey;
}
