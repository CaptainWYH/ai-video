package com.aivideo.agent;

import com.aivideo.common.BizException;
import com.aivideo.common.ResultCode;
import io.agentscope.core.model.transport.HttpTransportException;
import io.agentscope.extensions.model.openai.exception.OpenAIException;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;
import java.util.concurrent.TimeoutException;

/**
 * 将 AgentScope / 模型厂商异常翻译为业务异常。
 *
 * <p>鉴权失败映射为 502 而非 401，避免前端误判登录过期而触发刷新或跳转登录。</p>
 */
@Slf4j
public final class ModelErrorTranslator {

    private ModelErrorTranslator() {
    }

    public static BizException translate(Throwable error) {
        Integer status = null;
        boolean timeout = false;

        Throwable current = error;
        while (current != null) {
            if (current instanceof BizException bizException) {
                return bizException;
            }
            Integer currentStatus = statusOf(current);
            if (currentStatus != null) {
                status = currentStatus;
            }
            if (isTimeout(current)) {
                timeout = true;
            }
            current = current.getCause() == current ? null : current.getCause();
        }

        if (timeout) {
            log.warn("模型调用超时: type={}", error.getClass().getSimpleName());
            return new BizException(ResultCode.MODEL_TIMEOUT);
        }

        if (status != null) {
            if (status == 401 || status == 403) {
                log.warn("模型鉴权失败: type={}, status={}", error.getClass().getSimpleName(), status);
                return new BizException(ResultCode.MODEL_AUTH_FAILED);
            }
            if (status == 429) {
                log.warn("模型调用限流: status={}", status);
                return new BizException(ResultCode.MODEL_RATE_LIMITED);
            }
            if (status == 400 || status == 422) {
                log.warn("模型拒绝请求: status={}, message={}", status, error.getMessage());
                return BizException.badRequest("模型无法处理该请求，请调整内容后重试");
            }
            if (status >= 500) {
                log.warn("模型服务错误: type={}, status={}", error.getClass().getSimpleName(), status);
                return new BizException(ResultCode.MODEL_UNAVAILABLE);
            }
        }

        log.error("模型调用失败", error);
        return new BizException(ResultCode.MODEL_UNAVAILABLE);
    }

    private static Integer statusOf(Throwable error) {
        if (error instanceof OpenAIException openAIException) {
            return openAIException.getStatusCode();
        }
        if (error instanceof HttpTransportException transportException) {
            return transportException.getStatusCode();
        }
        return null;
    }

    private static boolean isTimeout(Throwable error) {
        if (error instanceof TimeoutException) {
            return true;
        }
        String message = error.getMessage();
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("timeout") || normalized.contains("timed out");
    }
}
