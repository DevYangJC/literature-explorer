package com.yuyuan.literature.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应状态码枚举
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // 成功
    SUCCESS(200, "操作成功"),

    // 客户端错误
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    CONFLICT(409, "资源冲突"),
    UNSUPPORTED_MEDIA_TYPE(415, "不支持的媒体类型"),
    VALIDATION_ERROR(422, "参数校验失败"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),

    // 服务器错误
    ERROR(500, "系统异常"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),

    // 业务错误码 (6xxx)
    BUSINESS_ERROR(6000, "业务处理失败"),
    DATA_NOT_EXIST(6001, "数据不存在"),
    DATA_ALREADY_EXIST(6002, "数据已存在"),
    OPERATION_NOT_ALLOWED(6003, "操作不被允许"),
    INSUFFICIENT_PERMISSIONS(6004, "权限不足"),
    ACCOUNT_DISABLED(6005, "账户已被禁用"),
    PASSWORD_ERROR(6006, "密码错误"),
    VERIFICATION_CODE_ERROR(6007, "验证码错误"),
    TOKEN_EXPIRED(6008, "令牌已过期"),
    TOKEN_INVALID(6009, "令牌无效"),
    VALIDATION_FAILED(6010, "参数验证失败"),

    // 文件相关错误 (7xxx)
    FILE_UPLOAD_ERROR(7001, "文件上传失败"),
    FILE_NOT_EXIST(7002, "文件不存在"),
    FILE_SIZE_EXCEEDED(7003, "文件大小超出限制"),
    FILE_TYPE_NOT_SUPPORTED(7004, "文件类型不支持"),

    // 数据库相关错误 (8xxx)
    DATABASE_ERROR(8001, "数据库操作异常"),
    DATA_INTEGRITY_VIOLATION(8002, "数据完整性违反"),
    DUPLICATE_KEY_ERROR(8003, "主键或唯一键冲突"),

    // 外部服务错误 (9xxx)
    THIRD_PARTY_SERVICE_ERROR(9001, "第三方服务异常"),
    NETWORK_ERROR(9002, "网络连接异常"),
    TIMEOUT_ERROR(9003, "请求超时");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 消息
     */
    private final String message;
}
