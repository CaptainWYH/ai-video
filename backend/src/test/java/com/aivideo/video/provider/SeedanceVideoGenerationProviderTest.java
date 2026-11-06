package com.aivideo.video.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeedanceVideoGenerationProviderTest {

    @Test
    void pollingRequestOmitsNullCallbackUrl() {
        SeedanceVideoProperties properties = new SeedanceVideoProperties();
        properties.setApiKey("test-key");
        SeedanceVideoGenerationProvider provider =
                new SeedanceVideoGenerationProvider(properties, new ObjectMapper());
        VideoProviderRequest request = new VideoProviderRequest(
                1L,
                7L,
                "海边日落",
                new VideoGenerationOptions(
                        "720p", "16:9", 5, true, false, "mp4"),
                null);

        String json = provider.serializeCreateRequestForTest(request);

        assertFalse(json.contains("callback_url"));
        assertTrue(json.contains("\"generate_audio\":true"));
        assertTrue(json.contains("\"output_format\":\"mp4\""));
    }
}
