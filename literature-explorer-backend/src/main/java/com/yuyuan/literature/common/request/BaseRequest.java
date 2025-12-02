package com.yuyuan.literature.common.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 基础请求类
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Data
@Schema(description = "基础请求参数")
public class BaseRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 请求ID，用于链路追踪
     */
    @Schema(description = "请求ID", hidden = true)
    private String requestId;

    /**
     * 客户端时间戳
     */
    @Schema(description = "客户端时间戳", example = "1640995200000")
    private Long timestamp;

    /**
     * 客户端版本
     */
    @Schema(description = "客户端版本", example = "1.0.0")
    private String clientVersion;

    /**
     * 设备类型
     */
    @Schema(description = "设备类型", example = "WEB")
    private String deviceType;
}
