package com.aivideo.video.provider;

public record VideoProviderRequest(
        Long localTaskId,
        Long userId,
        String prompt,
        VideoGenerationOptions options,
        String callbackUrl) {
}
