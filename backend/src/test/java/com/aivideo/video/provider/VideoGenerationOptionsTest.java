package com.aivideo.video.provider;

import com.aivideo.common.BizException;
import com.aivideo.video.dto.VideoGenerationParams;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VideoGenerationOptionsTest {

    @Test
    void usesSeedanceSafeDefaults() {
        VideoGenerationOptions options = VideoGenerationOptions.from(null);

        assertEquals("720p", options.resolution());
        assertEquals("16:9", options.ratio());
        assertEquals(5, options.durationSeconds());
        assertTrue(options.generateAudio());
    }

    @Test
    void mapsFrontendAspectToSeedanceRatio() {
        VideoGenerationOptions options = VideoGenerationOptions.from(
                new VideoGenerationParams("cinematic", "9:16", 15, "1080p", true, false, "mp4"));

        assertEquals("9:16", options.ratio());
        assertEquals(15, options.durationSeconds());
    }

    @Test
    void rejectsDurationOutsideSeedanceRange() {
        assertThrows(BizException.class,
                () -> VideoGenerationOptions.from(new VideoGenerationParams(
                        "cinematic", "16:9", 31, "720p", true, false, "mp4")));
    }

    @Test
    void rejectsUnsupportedResolution() {
        assertThrows(BizException.class,
                () -> VideoGenerationOptions.from(new VideoGenerationParams(
                        "cinematic", "16:9", 5, "4k", true, false, "mp4")));
    }
}
