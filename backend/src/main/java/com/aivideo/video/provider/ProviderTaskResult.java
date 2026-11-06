package com.aivideo.video.provider;

public record ProviderTaskResult(
        String providerTaskId,
        String providerStatus,
        String videoUrl,
        String errorCode,
        String errorMessage,
        Long completionTokens,
        Long totalTokens,
        Integer durationSeconds,
        String resolution,
        String ratio,
        Long seed) {
}
