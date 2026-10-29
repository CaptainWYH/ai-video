package com.aivideo.video.service;

import com.aivideo.common.PageResult;
import com.aivideo.video.dto.CreateVideoTaskRequest;
import com.aivideo.video.dto.VideoTaskResponse;

/**
 * 视频生成服务。本阶段为占位实现，不接入真实视频模型。
 */
public interface VideoGenerationService {

    VideoTaskResponse createTask(Long userId, CreateVideoTaskRequest request);

    VideoTaskResponse getTask(Long userId, Long taskId);

    PageResult<VideoTaskResponse> listTasks(Long userId, long page, long size);
}
