package com.aivideo.common;

import lombok.Getter;

/**
 * 业务异常。由 {@link GlobalExceptionHandler} 统一转换为 {@link Result}。
 */
@Getter
public class BizException extends RuntimeException {

    private final ResultCode code;

    public BizException(ResultCode code) {
        super(code.getMessage());
        this.code = code;
    }

    public BizException(ResultCode code, String message) {
        super(message);
        this.code = code;
    }

    public static BizException badRequest(String message) {
        return new BizException(ResultCode.BAD_REQUEST, message);
    }

    public static BizException notFound(String message) {
        return new BizException(ResultCode.NOT_FOUND, message);
    }

    public static BizException conflict(String message) {
        return new BizException(ResultCode.CONFLICT, message);
    }

    public static BizException unauthorized(String message) {
        return new BizException(ResultCode.UNAUTHORIZED, message);
    }

    public static BizException forbidden(String message) {
        return new BizException(ResultCode.FORBIDDEN, message);
    }
}
