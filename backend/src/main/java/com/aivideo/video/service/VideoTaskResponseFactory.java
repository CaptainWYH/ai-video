package com.aivideo.video.service;

import com.aivideo.storage.ObjectStorageService;
import com.aivideo.storage.StorageProperties;
import com.aivideo.video.dto.VideoGenerationParams;
import com.aivideo.video.dto.VideoTaskResponse;
import com.aivideo.video.dto.VideoUsageResponse;
import com.aivideo.video.entity.VideoTask;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;


@Slf4j
@Component
@RequiredArgsConstructor
public class VideoTaskResponseFactory {

    private final ObjectMapper objectMapper;
    private final ObjectStorageService objectStorageService;
    private final StorageProperties storageProperties;

    public VideoTaskResponse toResponse(VideoTask task) {
        VideoUsageResponse usage = task.getCompletionTokens() == null
                && task.getEstimatedCostCny() == null
                ? null
                : VideoUsageResponse.builder()
                .completionTokens(task.getCompletionTokens())
                .totalTokens(task.getTotalTokens())
                .unitPriceCnyPerMillion(task.getUnitPriceCnyPerMillion())
                .pricingVersion(task.getPricingVersion())
                .estimatedCostCny(task.getEstimatedCostCny())
                .currency(task.getCurrency())
                .build();
        return VideoTaskResponse.builder()
                .id(task.getId())
                .projectId(task.getProjectId())
                .prompt(task.getPrompt())
                .params(readParams(task.getParams()))
                .status(task.getStatus())
                .progress(task.getProgress())
                .provider(task.getProvider())
                .providerModel(task.getProviderModel())
                .providerStatus(task.getProviderStatus())
                .resultUrl(publishedUrl(task))
                .errorMessage(task.getErrorMessage())
                .usage(usage)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .completedAt(task.getCompletedAt())
                .build();
    }

    private String publishedUrl(VideoTask task) {
        if (StringUtils.hasText(task.getStorageKey())) {
            try {
                return objectStorageService.presignedUrl(
                        task.getStorageKey(), storageProperties.getPresignedTtl());
            } catch (Exception ex) {
                log.warn("生成视频预签名地址失败: taskId={}, error={}", task.getId(), ex.getMessage());
                return null;
            }
        }
        return task.getResultUrl();
    }

    private VideoGenerationParams readParams(String params) {
        if (!StringUtils.hasText(params)) {
            return VideoGenerationParams.defaults();
        }
        try {
            return objectMapper.readValue(params, VideoGenerationParams.class);
        } catch (Exception ex) {
            log.warn("视频参数解析失败: {}", ex.getMessage());
            return VideoGenerationParams.defaults();
        }
    }
}
