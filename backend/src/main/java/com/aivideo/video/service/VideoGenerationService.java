package com.aivideo.video.service;

import com.aivideo.common.PageResult;
import com.aivideo.video.dto.CreateVideoTaskRequest;
import com.aivideo.video.dto.VideoGenerationParams;
import com.aivideo.video.dto.VideoTaskResponse;
import com.aivideo.video.dto.VideoUsageSummaryResponse;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;

public interface VideoGenerationService {

    VideoTaskResponse createTask(Long userId, CreateVideoTaskRequest request);

    VideoTaskResponse getTask(Long userId, Long taskId);

    PageResult<VideoTaskResponse> listTasks(Long userId, long page, long size);

    VideoTaskResponse createFromChat(Long userId, Long sessionId, Long messageId,
                                     String prompt, VideoGenerationParams params,
                                     String idempotencyKey);

    VideoTaskResponse linkTaskToChat(Long userId, Long taskId,
                                     Long sessionId, Long messageId);

    Map<Long, VideoTaskResponse> findTasksForChatMessages(Long userId,
                                                          Collection<Long> messageIds);

    VideoTaskResponse cancelTask(Long userId, Long taskId);

    VideoTaskResponse retryTask(Long userId, Long taskId);

    VideoUsageSummaryResponse usageSummary(Long userId, LocalDate from, LocalDate to);
}
