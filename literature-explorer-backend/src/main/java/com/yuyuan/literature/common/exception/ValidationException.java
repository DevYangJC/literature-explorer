package com.yuyuan.literature.common.exception;

import com.yuyuan.literature.common.result.ResultCode;

/**
 * 参数校验异常类
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
public class ValidationException extends BaseException {

    private static final long serialVersionUID = 1L;

    /**
     * 构造方法
     */
    public ValidationException(String message) {
        super(ResultCode.VALIDATION_ERROR.getCode(), message);
    }

    /**
     * 构造方法
     */
    public ValidationException(String message, Throwable cause) {
        super(ResultCode.VALIDATION_ERROR.getCode(), message, cause);
    }

    /**
     * 构造方法
     */
    public ValidationException(ResultCode resultCode) {
        super(resultCode);
    }

    /**
     * 构造方法
     */
    public ValidationException(ResultCode resultCode, Throwable cause) {
        super(resultCode, cause);
    }
}
