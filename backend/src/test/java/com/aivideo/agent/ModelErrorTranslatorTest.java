package com.aivideo.agent;

import com.aivideo.common.BizException;
import com.aivideo.common.ResultCode;
import io.agentscope.core.model.transport.HttpTransportException;
import io.agentscope.extensions.model.openai.exception.OpenAIException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ModelErrorTranslatorTest {

    @Test
    void mapsUnauthorizedToAuthFailed() {
        BizException result = ModelErrorTranslator.translate(OpenAIException.create(401, "unauthorized", null, null));

        assertEquals(ResultCode.MODEL_AUTH_FAILED, result.getCode());
    }

    @Test
    void mapsForbiddenToAuthFailed() {
        BizException result = ModelErrorTranslator.translate(OpenAIException.create(403, "forbidden", null, null));

        assertEquals(ResultCode.MODEL_AUTH_FAILED, result.getCode());
    }

    @Test
    void mapsRateLimit() {
        BizException result = ModelErrorTranslator.translate(OpenAIException.create(429, "rate limited", null, null));

        assertEquals(ResultCode.MODEL_RATE_LIMITED, result.getCode());
    }

    @Test
    void mapsServerErrorToUnavailable() {
        BizException result = ModelErrorTranslator.translate(OpenAIException.create(500, "server error", null, null));

        assertEquals(ResultCode.MODEL_UNAVAILABLE, result.getCode());
    }

    @Test
    void mapsTransportErrorByStatusCode() {
        BizException result = ModelErrorTranslator.translate(
                new HttpTransportException("connect failed", 503, "unavailable"));

        assertEquals(ResultCode.MODEL_UNAVAILABLE, result.getCode());
    }

    @Test
    void mapsTimeout() {
        BizException result = ModelErrorTranslator.translate(new TimeoutException("read timed out"));

        assertEquals(ResultCode.MODEL_TIMEOUT, result.getCode());
    }

    @Test
    void mapsWrappedTimeout() {
        BizException result = ModelErrorTranslator.translate(
                new RuntimeException("wrapped", new TimeoutException("timeout")));

        assertEquals(ResultCode.MODEL_TIMEOUT, result.getCode());
    }

    @Test
    void keepsBizExceptionAsIs() {
        BizException original = new BizException(ResultCode.MODEL_UNAVAILABLE, "custom");

        assertSame(original, ModelErrorTranslator.translate(original));
    }
}
