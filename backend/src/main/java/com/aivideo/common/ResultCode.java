package com.aivideo.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 业务错误码。code 为 0 表示成功，非 0 为业务错误；HTTP 状态码与之对应。
 */
@Getter
public enum ResultCode {

    SUCCESS(0, "success", HttpStatus.OK),
    BAD_REQUEST(40000, "请求参数错误", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(40100, "未登录或登录已过期", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(40300, "没有权限执行该操作", HttpStatus.FORBIDDEN),
    NOT_FOUND(40400, "资源不存在", HttpStatus.NOT_FOUND),
    CONFLICT(40900, "资源冲突", HttpStatus.CONFLICT),
    MODEL_RATE_LIMITED(42900, "模型调用过于频繁，请稍后重试", HttpStatus.TOO_MANY_REQUESTS),
    INTERNAL_ERROR(50000, "服务器内部错误", HttpStatus.INTERNAL_SERVER_ERROR),
    MODEL_AUTH_FAILED(50200, "模型服务鉴权失败，请联系管理员", HttpStatus.BAD_GATEWAY),
    MODEL_UNAVAILABLE(50300, "模型服务暂不可用，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE),
    MODEL_TIMEOUT(50400, "模型响应超时，请稍后重试", HttpStatus.GATEWAY_TIMEOUT);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ResultCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
