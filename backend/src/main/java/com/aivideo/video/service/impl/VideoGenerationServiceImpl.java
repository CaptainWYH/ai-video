package com.aivideo.video.service.impl;

import com.aivideo.common.BizException;
import com.aivideo.common.PageResult;
import com.aivideo.project.entity.Project;
import com.aivideo.project.mapper.ProjectMapper;
import com.aivideo.video.dto.CreateVideoTaskRequest;
import com.aivideo.video.dto.VideoGenerationParams;
import com.aivideo.video.dto.VideoTaskResponse;
import com.aivideo.video.dto.VideoUsageSummaryResponse;
import com.aivideo.video.entity.VideoTask;
import com.aivideo.video.entity.VideoTaskStatus;
import com.aivideo.video.mapper.VideoTaskMapper;
import com.aivideo.video.provider.VideoGenerationOptions;
import com.aivideo.video.provider.VideoProviderRouter;
import com.aivideo.video.service.VideoGenerationService;
import com.aivideo.video.service.VideoPricingService;
import com.aivideo.video.service.VideoTaskResponseFactory;
import com.aivideo.websocket.ChatPushService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoGenerationServiceImpl implements VideoGenerationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final VideoTaskMapper videoTaskMapper;
    private final ProjectMapper projectMapper;
    private final ObjectMapper objectMapper;
    private final VideoProviderRouter providerRouter;
    private final VideoPricingService pricingService;
    private final VideoTaskResponseFactory responseFactory;
    private final ChatPushService chatPushService;

    @Override
    @Transactional
    public VideoTaskResponse createTask(Long userId, CreateVideoTaskRequest request) {
        return createTask(userId, request, null, null);
    }

    @Override
    @Transactional
    public VideoTaskResponse createFromChat(Long userId, Long sessionId, Long messageId,
                                            String prompt, VideoGenerationParams params,
                                            String idempotencyKey) {
        CreateVideoTaskRequest request = new CreateVideoTaskRequest();
        request.setPrompt(prompt);
        request.setParams(params);
        request.setClientRequestId(idempotencyKey);
        return createTask(userId, request, sessionId, messageId);
    }

    @Override
    @Transactional
    public VideoTaskResponse linkTaskToChat(Long userId, Long taskId,
                                            Long sessionId, Long messageId) {
        VideoTask task = getOwnedTask(userId, taskId);
        if (task.getChatSessionId() != null && !task.getChatSessionId().equals(sessionId)) {
            throw BizException.conflict("视频任务不属于当前会话");
        }
        if (task.getChatMessageId() != null && !task.getChatMessageId().equals(messageId)) {
            throw BizException.conflict("视频任务已经关联其他消息");
        }
        task.setChatSessionId(sessionId);
        task.setChatMessageId(messageId);
        task.setNextPollAt(LocalDateTime.now());
        videoTaskMapper.updateById(task);
        return responseFactory.toResponse(task);
    }

    private VideoTaskResponse createTask(Long userId, CreateVideoTaskRequest request,
                                         Long chatSessionId, Long chatMessageId) {
        if (request.getProjectId() != null) {
            Project project = projectMapper.selectById(request.getProjectId());
            if (project == null || !project.getUserId().equals(userId)) {
                throw BizException.notFound("项目不存在");
            }
        }

        String clientRequestId = StringUtils.hasText(request.getClientRequestId())
                ? request.getClientRequestId().trim()
                : UUID.randomUUID().toString();
        VideoTask existing = findByIdempotencyKey(userId, clientRequestId);
        if (existing != null) {
            return responseFactory.toResponse(existing);
        }

        VideoGenerationOptions options = VideoGenerationOptions.from(request.getParams());
        VideoPricingService.PriceQuote price = "seedance".equals(providerRouter.providerName())
                ? pricingService.quote(providerRouter.model(), options)
                : new VideoPricingService.PriceQuote(null, null, "CNY");
        VideoTask task = new VideoTask();
        task.setUserId(userId);
        task.setProjectId(request.getProjectId());
        task.setChatSessionId(chatSessionId);
        task.setChatMessageId(chatMessageId);
        task.setPrompt(request.getPrompt().trim());
        task.setParams(writeParams(request.getParams()));
        task.setStatus(VideoTaskStatus.PENDING.name());
        task.setProgress(0);
        task.setProvider(providerRouter.providerName().toUpperCase());
        task.setProviderModel(providerRouter.model());
        task.setClientRequestId(clientRequestId);
        task.setUnitPriceCnyPerMillion(price.unitPriceCnyPerMillion());
        task.setPricingVersion(price.pricingVersion());
        task.setCurrency(price.currency());
        task.setAttemptCount(0);
        task.setNextPollAt(LocalDateTime.now());
        try {
            videoTaskMapper.insert(task);
        } catch (DuplicateKeyException ex) {
            existing = findByIdempotencyKey(userId, clientRequestId);
            if (existing != null) {
                return responseFactory.toResponse(existing);
            }
            throw ex;
        }
        return responseFactory.toResponse(task);
    }

    @Override
    public VideoTaskResponse getTask(Long userId, Long taskId) {
        return responseFactory.toResponse(getOwnedTask(userId, taskId));
    }

    @Override
    public PageResult<VideoTaskResponse> listTasks(Long userId, long page, long size) {
        Page<VideoTask> pageRequest = new Page<>(Math.max(1, page),
                Math.min(MAX_PAGE_SIZE, Math.max(1, size)));
        LambdaQueryWrapper<VideoTask> wrapper = new LambdaQueryWrapper<VideoTask>()
                .eq(VideoTask::getUserId, userId)
                .orderByDesc(VideoTask::getId);
        Page<VideoTask> result = videoTaskMapper.selectPage(pageRequest, wrapper);
        List<VideoTaskResponse> records = result.getRecords().stream()
                .map(responseFactory::toResponse)
                .toList();
        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @Override
    public Map<Long, VideoTaskResponse> findTasksForChatMessages(Long userId,
                                                                 Collection<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, VideoTaskResponse> result = new LinkedHashMap<>();
        videoTaskMapper.findByChatMessageIds(userId, messageIds.stream().toList())
                .forEach(task -> result.putIfAbsent(task.getChatMessageId(),
                        responseFactory.toResponse(task)));
        return result;
    }

    @Override
    public VideoTaskResponse cancelTask(Long userId, Long taskId) {
        VideoTask task = getOwnedTask(userId, taskId);
        if (!List.of(VideoTaskStatus.PENDING.name(), VideoTaskStatus.QUEUED.name())
                .contains(task.getStatus())) {
            throw BizException.conflict("仅排队中的任务可以取消");
        }
        if (StringUtils.hasText(task.getProviderTaskId())
                && VideoTaskStatus.QUEUED.name().equals(task.getStatus())) {
            providerRouter.current().cancel(task.getProviderTaskId());
        }
        task.setStatus(VideoTaskStatus.CANCELLED.name());
        task.setProgress(0);
        task.setCompletedAt(LocalDateTime.now());
        task.setLeaseUntil(null);
        task.setNextPollAt(null);
        videoTaskMapper.updateById(task);
        publish(task);
        return responseFactory.toResponse(task);
    }

    @Override
    @Transactional
    public VideoTaskResponse retryTask(Long userId, Long taskId) {
        VideoTask source = getOwnedTask(userId, taskId);
        CreateVideoTaskRequest request = new CreateVideoTaskRequest();
        request.setPrompt(source.getPrompt());
        request.setProjectId(source.getProjectId());
        request.setParams(readParams(source.getParams()));
        request.setClientRequestId("retry:" + source.getId() + ":" + UUID.randomUUID());
        return createTask(userId, request, source.getChatSessionId(), source.getChatMessageId());
    }

    @Override
    public VideoUsageSummaryResponse usageSummary(Long userId, LocalDate from, LocalDate to) {
        LambdaQueryWrapper<VideoTask> wrapper = new LambdaQueryWrapper<VideoTask>()
                .eq(VideoTask::getUserId, userId);
        if (from != null) {
            wrapper.ge(VideoTask::getCreatedAt, from.atStartOfDay());
        }
        if (to != null) {
            wrapper.lt(VideoTask::getCreatedAt, to.plusDays(1).atStartOfDay());
        }
        List<VideoTask> tasks = videoTaskMapper.selectList(wrapper);
        long successCount = tasks.stream()
                .filter(task -> VideoTaskStatus.SUCCESS.name().equals(task.getStatus()))
                .count();
        long tokens = tasks.stream()
                .map(VideoTask::getCompletionTokens)
                .filter(java.util.Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();
        BigDecimal cost = tasks.stream()
                .map(VideoTask::getEstimatedCostCny)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return VideoUsageSummaryResponse.builder()
                .taskCount(tasks.size())
                .successCount(successCount)
                .completionTokens(tokens)
                .estimatedCostCny(cost)
                .currency("CNY")
                .build();
    }

    private VideoTask getOwnedTask(Long userId, Long taskId) {
        VideoTask task = videoTaskMapper.selectById(taskId);
        if (task == null || !task.getUserId().equals(userId)) {
            throw BizException.notFound("视频任务不存在");
        }
        return task;
    }

    private VideoTask findByIdempotencyKey(Long userId, String clientRequestId) {
        return videoTaskMapper.selectOne(new LambdaQueryWrapper<VideoTask>()
                .eq(VideoTask::getUserId, userId)
                .eq(VideoTask::getClientRequestId, clientRequestId)
                .last("LIMIT 1"));
    }

    private String writeParams(VideoGenerationParams params) {
        try {
            return objectMapper.writeValueAsString(
                    params == null ? VideoGenerationParams.defaults() : params);
        } catch (Exception ex) {
            throw new IllegalArgumentException("视频参数序列化失败", ex);
        }
    }

    private VideoGenerationParams readParams(String params) {
        if (!StringUtils.hasText(params)) {
            return VideoGenerationParams.defaults();
        }
        try {
            return objectMapper.readValue(params, VideoGenerationParams.class);
        } catch (Exception ex) {
            throw new IllegalStateException("视频参数解析失败", ex);
        }
    }

    private void publish(VideoTask task) {
        VideoTaskResponse response = responseFactory.toResponse(task);
        chatPushService.pushVideoProgress(
                task.getUserId(),
                task.getId(),
                task.getChatSessionId(),
                task.getChatMessageId(),
                task.getStatus(),
                task.getProgress(),
                response.getResultUrl(),
                response.getUsage());
    }
}
