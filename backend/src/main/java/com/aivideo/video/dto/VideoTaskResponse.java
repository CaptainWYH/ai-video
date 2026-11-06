package com.aivideo.video.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoTaskResponse {

    private Long id;
    private Long projectId;
    private String prompt;
    private VideoGenerationParams params;
    private String status;
    private Integer progress;
    private String provider;
    private String providerModel;
    private String providerStatus;
    private String resultUrl;
    private String errorMessage;
    private VideoUsageResponse usage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
}
