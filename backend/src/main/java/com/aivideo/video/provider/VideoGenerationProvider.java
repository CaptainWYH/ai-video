package com.aivideo.video.provider;

public interface VideoGenerationProvider {

    String name();

    ProviderSubmission submit(VideoProviderRequest request);

    ProviderTaskResult query(String providerTaskId);

    void cancel(String providerTaskId);
}
