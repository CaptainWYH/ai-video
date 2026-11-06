package com.aivideo.video.provider;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockVideoGenerationProvider implements VideoGenerationProvider {

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public ProviderSubmission submit(VideoProviderRequest request) {
        return new ProviderSubmission(
                "mock-" + request.localTaskId() + "-" + System.currentTimeMillis() + "-"
                        + UUID.randomUUID(),
                "queued");
    }

    @Override
    public ProviderTaskResult query(String providerTaskId) {
        String[] parts = providerTaskId.split("-");
        long localTaskId = Long.parseLong(parts[1]);
        long submittedAt = Long.parseLong(parts[2]);
        if (System.currentTimeMillis() - submittedAt < 1800) {
            return new ProviderTaskResult(providerTaskId, "running", null, null, null,
                    null, null, 5, "720p", "16:9", null);
        }
        return new ProviderTaskResult(providerTaskId, "succeeded",
                "https://example.com/mock-video/" + localTaskId + ".mp4",
                null, null, 0L, 0L, 5, "720p", "16:9", null);
    }

    @Override
    public void cancel(String providerTaskId) {
        // Mock 任务无需服务端资源清理。
    }
}
