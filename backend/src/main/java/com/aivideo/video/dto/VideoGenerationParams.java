package com.aivideo.video.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/**
 * 视频生成参数。所有入口使用同一组显式字段，避免任意 Map 透传。
 */
public record VideoGenerationParams(
        @Pattern(regexp = "cinematic|realistic|anime|cyberpunk|ink",
                message = "style 取值不合法") String style,
        @Pattern(regexp = "16:9|9:16|1:1|4:3|3:4|21:9|adaptive",
                message = "aspect 取值不合法") String aspect,
        @Min(value = 4, message = "duration 不能小于 4 秒")
        @Max(value = 30, message = "duration 不能超过 30 秒") Integer duration,
        @Pattern(regexp = "480p|720p|1080p", message = "resolution 仅支持 480p、720p、1080p")
        String resolution,
        Boolean generateAudio,
        Boolean watermark,
        @Pattern(regexp = "mp4|mov", message = "outputFormat 仅支持 mp4 或 mov")
        String outputFormat) {

    public static VideoGenerationParams defaults() {
        return new VideoGenerationParams(
                "cinematic", "16:9", 5, "720p", true, false, "mp4");
    }

    public VideoGenerationParams {
        style = style == null ? "cinematic" : style;
        aspect = aspect == null ? "16:9" : aspect;
        duration = duration == null ? 5 : duration;
        resolution = resolution == null ? "720p" : resolution;
        generateAudio = generateAudio == null || generateAudio;
        watermark = watermark != null && watermark;
        outputFormat = outputFormat == null ? "mp4" : outputFormat;
    }

    public VideoGenerationParams withDuration(Integer value) {
        return withToolFallbacks(null, null, value, null, null, null, null);
    }

    /**
     * 界面/API 已提供的参数优先；工具参数只补齐缺失字段。
     */
    public VideoGenerationParams withToolFallbacks(
            String newStyle,
            String newAspect,
            Integer newDuration,
            String newResolution,
            Boolean newGenerateAudio,
            Boolean newWatermark,
            String newOutputFormat) {
        return new VideoGenerationParams(
                style != null ? style : newStyle,
                aspect != null ? aspect : newAspect,
                duration != null ? duration : newDuration,
                resolution != null ? resolution : newResolution,
                generateAudio != null ? generateAudio : newGenerateAudio,
                watermark != null ? watermark : newWatermark,
                outputFormat != null ? outputFormat : newOutputFormat);
    }
}
