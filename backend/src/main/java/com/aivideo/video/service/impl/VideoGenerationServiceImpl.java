package com.aivideo.video.service.impl;

import com.aivideo.common.BizException;
import com.aivideo.common.PageResult;
import com.aivideo.project.entity.Project;
import com.aivideo.project.mapper.ProjectMapper;
import com.aivideo.video.dto.CreateVideoTaskRequest;
import com.aivideo.video.dto.VideoTaskResponse;
import com.aivideo.video.entity.VideoTask;
import com.aivideo.video.entity.VideoTaskStatus;
import com.aivideo.video.mapper.VideoTaskMapper;
import com.aivideo.video.service.VideoGenerationService;
import com.aivideo.websocket.ChatPushService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 视频生成占位实现。
 *
 * <p>TODO 待接入：
 * <ol>
 *   <li>真实视频模型（如通义万相 / Runway / 自建推理服务）；</li>
 *   <li>异步任务队列（Redis Stream / MQ）与失败重试；</li>
 *   <li>生成结果写入 MinIO/S3 并返回预签名 URL。</li>
 * </ol>
 */
@Slf4j
@Service
public class VideoGenerationServiceImpl implements VideoGenerationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final VideoTaskMapper videoTaskMapper;
    private final ProjectMapper projectMapper;
    private final ChatPushService chatPushService;
    private final ObjectMapper objectMapper;
    private final Executor videoTaskExecutor;

    public VideoGenerationServiceImpl(VideoTaskMapper videoTaskMapper,
                                      ProjectMapper projectMapper,
                                      ChatPushService chatPushService,
                                      ObjectMapper objectMapper,
                                      @Qualifier("videoTaskExecutor") Executor videoTaskExecutor) {
        this.videoTaskMapper = videoTaskMapper;
        this.projectMapper = projectMapper;
        this.chatPushService = chatPushService;
        this.objectMapper = objectMapper;
        this.videoTaskExecutor = videoTaskExecutor;
    }

    @Override
    @Transactional
    public VideoTaskResponse createTask(Long userId, CreateVideoTaskRequest request) {
        if (request.getProjectId() != null) {
            Project project = projectMapper.selectById(request.getProjectId());
            if (project == null || !project.getUserId().equals(userId)) {
                throw BizException.notFound("项目不存在");
            }
        }

        VideoTask task = new VideoTask();
        task.setUserId(userId);
        task.setProjectId(request.getProjectId());
        task.setPrompt(request.getPrompt());
        task.setParams(writeParams(request.getParams()));
        task.setStatus(VideoTaskStatus.PENDING.name());
        task.setProgress(0);
        videoTaskMapper.insert(task);

        videoTaskExecutor.execute(() -> simulateGeneration(task.getId()));
        return toResponse(task);
    }

    @Override
    public VideoTaskResponse getTask(Long userId, Long taskId) {
        return toResponse(getOwnedTask(userId, taskId));
    }

    @Override
    public PageResult<VideoTaskResponse> listTasks(Long userId, long page, long size) {
        Page<VideoTask> pageRequest = new Page<>(Math.max(1, page), Math.min(MAX_PAGE_SIZE, Math.max(1, size)));
        LambdaQueryWrapper<VideoTask> wrapper = new LambdaQueryWrapper<VideoTask>()
                .eq(VideoTask::getUserId, userId)
                .orderByDesc(VideoTask::getId);
        Page<VideoTask> result = videoTaskMapper.selectPage(pageRequest, wrapper);
        List<VideoTaskResponse> records = result.getRecords().stream()
                .map(this::toResponse)
                .toList();
        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    private VideoTask getOwnedTask(Long userId, Long taskId) {
        VideoTask task = videoTaskMapper.selectById(taskId);
        if (task == null || !task.getUserId().equals(userId)) {
            throw BizException.notFound("视频任务不存在");
        }
        return task;
    }

    /**
     * 模拟异步生成：PENDING -> RUNNING(30) -> RUNNING(70) -> SUCCESS(100)。
     * 每次状态变化通过 WebSocket 推送 video.progress 事件。
     */
    private void simulateGeneration(Long taskId) {
        try {
            Thread.sleep(600);
            updateProgress(taskId, VideoTaskStatus.RUNNING, 30, null);

            Thread.sleep(900);
            updateProgress(taskId, VideoTaskStatus.RUNNING, 70, null);

            Thread.sleep(900);
            updateProgress(taskId, VideoTaskStatus.SUCCESS, 100,
                    "https://example.com/mock-video/" + taskId + ".mp4");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("视频任务模拟被中断: taskId={}", taskId);
        } catch (Exception ex) {
            log.error("视频任务模拟失败: taskId={}", taskId, ex);
            updateStatus(taskId, VideoTaskStatus.FAILED, 0, null, ex.getMessage());
        }
    }

    private void updateProgress(Long taskId, VideoTaskStatus status, int progress, String resultUrl) {
        updateStatus(taskId, status, progress, resultUrl, null);
    }

    private void updateStatus(Long taskId, VideoTaskStatus status, int progress,
                              String resultUrl, String errorMessage) {
        VideoTask task = videoTaskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setStatus(status.name());
        task.setProgress(progress);
        if (StringUtils.hasText(resultUrl)) {
            task.setResultUrl(resultUrl);
        }
        if (StringUtils.hasText(errorMessage)) {
            task.setErrorMessage(errorMessage);
        }
        videoTaskMapper.updateById(task);
        chatPushService.pushVideoProgress(task.getUserId(), taskId, status.name(), progress,
                task.getResultUrl());
    }

    private String writeParams(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(params);
        } catch (Exception ex) {
            log.warn("视频参数序列化失败: {}", ex.getMessage());
            return null;
        }
    }

    private Map<String, Object> readParams(String params) {
        if (!StringUtils.hasText(params)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(params, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception ex) {
            log.warn("视频参数解析失败: {}", ex.getMessage());
            return Map.of();
        }
    }

    private VideoTaskResponse toResponse(VideoTask task) {
        return VideoTaskResponse.builder()
                .id(task.getId())
                .projectId(task.getProjectId())
                .prompt(task.getPrompt())
                .params(readParams(task.getParams()))
                .status(task.getStatus())
                .progress(task.getProgress())
                .resultUrl(task.getResultUrl())
                .errorMessage(task.getErrorMessage())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
