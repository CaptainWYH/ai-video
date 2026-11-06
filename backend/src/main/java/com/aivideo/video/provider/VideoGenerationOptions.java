package com.aivideo.video.provider;

import com.aivideo.common.BizException;
import com.aivideo.video.dto.VideoGenerationParams;

import java.util.Locale;
import java.util.Set;

public record VideoGenerationOptions(
        String resolution,
        String ratio,
        int durationSeconds,
        boolean generateAudio,
        boolean watermark,
        String outputFormat) {

    private static final Set<String> RESOLUTIONS = Set.of("480p", "720p", "1080p");
    private static final Set<String> RATIOS = Set.of("16:9", "4:3", "1:1", "3:4", "9:16", "21:9", "adaptive");
    private static final Set<String> STYLES = Set.of(
            "cinematic", "realistic", "anime", "cyberpunk", "ink");

    public static VideoGenerationOptions from(VideoGenerationParams input) {
        VideoGenerationParams safe = input == null ? VideoGenerationParams.defaults() : input;
        if (!STYLES.contains(safe.style())) {
            throw BizException.badRequest("style 取值不合法");
        }
        String resolution = safe.resolution().toLowerCase(Locale.ROOT);
        if (!RESOLUTIONS.contains(resolution)) {
            throw BizException.badRequest("resolution 仅支持 480p、720p、1080p");
        }

        String ratio = safe.aspect();
        if (!RATIOS.contains(ratio)) {
            throw BizException.badRequest("ratio 取值不合法");
        }

        int duration = safe.duration();
        if (duration < 4 || duration > 30) {
            throw BizException.badRequest("duration 必须在 4 到 30 秒之间");
        }

        String outputFormat = safe.outputFormat().toLowerCase(Locale.ROOT);
        if (!"mp4".equals(outputFormat) && !"mov".equals(outputFormat)) {
            throw BizException.badRequest("outputFormat 仅支持 mp4 或 mov");
        }

        return new VideoGenerationOptions(
                resolution,
                ratio,
                duration,
                safe.generateAudio(),
                safe.watermark(),
                outputFormat);
    }

    public boolean hasInputVideo() {
        return false;
    }

}
