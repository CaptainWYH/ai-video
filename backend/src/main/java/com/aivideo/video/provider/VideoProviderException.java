package com.aivideo.video.provider;

import lombok.Getter;

@Getter
public class VideoProviderException extends RuntimeException {

    private final Integer httpStatus;
    private final String providerCode;
    private final boolean retryable;

    public VideoProviderException(String message, Integer httpStatus, String providerCode, boolean retryable) {
        super(message);
        this.httpStatus = httpStatus;
        this.providerCode = providerCode;
        this.retryable = retryable;
    }
}
