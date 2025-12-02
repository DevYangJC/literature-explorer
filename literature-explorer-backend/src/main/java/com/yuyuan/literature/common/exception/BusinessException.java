package com.yuyuan.literature.common.exception;

import com.yuyuan.literature.common.result.ResultCode;

/**
 * 业务异常类
 *
 * @author Literature Assistant
 * @since 1.0.0
 */
public class BusinessException extends BaseException {

    private static final long serialVersionUID = 1L;

    /**
     * 构造方法
     */
    public BusinessException(String message) {
        super(ResultCode.BUSINESS_ERROR.getCode(), message);
    }

    /**
     * 构造方法
     */
    public BusinessException(String message, Throwable cause) {
        super(ResultCode.BUSINESS_ERROR.getCode(), message, cause);
    }

    /**
     * 构造方法
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode);
    }

    /**
     * 构造方法
     */
    public BusinessException(ResultCode resultCode, Throwable cause) {
        super(resultCode, cause);
    }

    /**
     * 构造方法
     */
    public BusinessException(Integer code, String message) {
        super(code, message);
    }

    /**
     * 构造方法
     */
    public BusinessException(Integer code, String message, Throwable cause) {
        super(code, message, cause);
    }

    /**
     * 构造方法
     */
    public BusinessException(ResultCode resultCode, String message) {
        super(resultCode.getCode(), message);
    }
}
