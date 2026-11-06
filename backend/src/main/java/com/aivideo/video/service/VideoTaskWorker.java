package com.aivideo.video.service;

import com.aivideo.video.dto.VideoTaskResponse;
import com.aivideo.video.dto.VideoGenerationParams;
import com.aivideo.video.entity.VideoTask;
import com.aivideo.video.entity.VideoTaskStatus;
import com.aivideo.video.mapper.VideoTaskMapper;
import com.aivideo.video.provider.ProviderSubmission;
import com.aivideo.video.provider.ProviderTaskResult;
import com.aivideo.video.provider.SeedanceCallbackSigner;
import com.aivideo.video.provider.SeedanceVideoProperties;
import com.aivideo.video.provider.VideoGenerationOptions;
import com.aivideo.video.provider.VideoProviderException;
import com.aivideo.video.provider.VideoProviderRequest;
import com.aivideo.video.provider.VideoProviderRouter;
import com.aivideo.websocket.ChatPushService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoTaskWorker {

    private final VideoTaskMapper videoTaskMapper;
    private final VideoProviderRouter providerRouter;
    private final SeedanceVideoProperties seedanceProperties;
    private final SeedanceCallbackSigner callbackSigner;
    private final VideoAssetService videoAssetService;
    private final VideoPricingService pricingService;
    private final VideoTaskResponseFactory responseFactory;
    private final ChatPushService chatPushService;
    private final ObjectMapper objectMapper;

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(fixedDelayString = "${app.video.worker-interval-ms:5000}")
    public void processDueTasks() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            LocalDateTime now = LocalDateTime.now();
            List<VideoTask> tasks = videoTaskMapper.findDueTasks(
                    now, Math.max(1, seedanceProperties.getMaxBatchSize()));
            for (VideoTask task : tasks) {
                processOne(task, now);
            }
        } catch (Exception ex) {
            log.error("视频任务调度失败", ex);
        } finally {
            running.set(false);
        }
    }

    public void refreshNow(Long taskId) {
        videoTaskMapper.update(null, new LambdaUpdateWrapper<VideoTask>()
                .eq(VideoTask::getId, taskId)
                .in(VideoTask::getStatus,
                        VideoTaskStatus.PENDING.name(),
                        VideoTaskStatus.SUBMITTING.name(),
                        VideoTaskStatus.QUEUED.name(),
                        VideoTaskStatus.RUNNING.name(),
                        VideoTaskStatus.DOWNLOADING.name())
                .set(VideoTask::getNextPollAt, LocalDateTime.now()));
    }

    private void processOne(VideoTask task, LocalDateTime now) {
        LocalDateTime leaseUntil = now.plus(seedanceProperties.getWorkerLease());
        int claimed = videoTaskMapper.claimTask(task.getId(), task.getStatus(), now, leaseUntil);
        if (claimed == 0) {
            return;
        }
        try {
            switch (VideoTaskStatus.valueOf(task.getStatus())) {
                case PENDING -> submit(task);
                case SUBMITTING -> {
                    if (StringUtils.hasText(task.getProviderTaskId())) {
                        task.setStatus(VideoTaskStatus.QUEUED.name());
                        videoTaskMapper.updateById(task);
                        refreshProviderState(task);
                    } else {
                        fail(task, "提交结果未知，请先到火山方舟控制台确认，避免重复扣费");
                    }
                }
                case DOWNLOADING -> finishDownload(task);
                case QUEUED, RUNNING -> refreshProviderState(task);
                default -> {
                    // 非活动状态不应进入调度。
                }
            }
        } catch (Exception ex) {
            log.error("处理视频任务失败: taskId={}", task.getId(), ex);
            handleWorkerFailure(task, ex);
        }
    }

    private void submit(VideoTask task) {
        task.setStatus(VideoTaskStatus.SUBMITTING.name());
        task.setProgress(5);
        task.setLeaseUntil(LocalDateTime.now().plus(seedanceProperties.getWorkerLease()));
        task.setNextPollAt(task.getLeaseUntil());
        videoTaskMapper.updateById(task);

        String callbackUrl = providerRouter.callbackConfigured()
                ? callbackSigner.callbackUrl(task.getId())
                : null;
        VideoGenerationParams params = readParams(task.getParams());
        VideoProviderRequest request = new VideoProviderRequest(
                task.getId(),
                task.getUserId(),
                composePrompt(task.getPrompt(), params),
                VideoGenerationOptions.from(params),
                callbackUrl);
        ProviderSubmission submission = providerRouter.current().submit(request);

        task.setProviderTaskId(submission.providerTaskId());
        task.setProviderStatus(submission.providerStatus());
        task.setStatus(mappedStatus(submission.providerStatus()));
        task.setProgress(10);
        task.setSubmittedAt(LocalDateTime.now());
        task.setLastPolledAt(null);
        task.setAttemptCount(1);
        task.setNextPollAt(nextPollTime());
        releaseLease(task);
        task.setErrorMessage(null);
        videoTaskMapper.updateById(task);
        publish(task);
    }

    private void refreshProviderState(VideoTask task) {
        if (!StringUtils.hasText(task.getProviderTaskId())) {
            fail(task, "缺少 Seedance 任务 ID");
            return;
        }
        ProviderTaskResult result = providerRouter.current().query(task.getProviderTaskId());
        log.info("本地视频任务查询: taskId={}, provider={}, providerTaskId={}, status={}, videoUrl={}",
                task.getId(),
                task.getProvider(),
                task.getProviderTaskId(),
                result.providerStatus(),
                result.videoUrl());
        task.setProviderStatus(result.providerStatus());
        task.setProviderErrorCode(result.errorCode());
        task.setLastPolledAt(LocalDateTime.now());
        task.setAttemptCount((task.getAttemptCount() == null ? 0 : task.getAttemptCount()) + 1);
        releaseLease(task);

        String status = result.providerStatus() == null
                ? "running" : result.providerStatus().toLowerCase(Locale.ROOT);
        switch (status) {
            case "succeeded" -> prepareDownload(task, result);
            case "failed" -> fail(task, providerErrorMessage(result));
            case "cancelled" -> finish(task, VideoTaskStatus.CANCELLED, 0, null);
            case "expired" -> finish(task, VideoTaskStatus.EXPIRED, 0,
                    "视频生成任务已超时");
            case "queued" -> {
                task.setStatus(VideoTaskStatus.QUEUED.name());
                task.setProgress(10);
                task.setNextPollAt(nextPollTime());
                videoTaskMapper.updateById(task);
                publish(task);
            }
            default -> {
                task.setStatus(VideoTaskStatus.RUNNING.name());
                task.setProgress(35);
                task.setNextPollAt(nextPollTime());
                videoTaskMapper.updateById(task);
                publish(task);
            }
        }
    }

    private void prepareDownload(VideoTask task, ProviderTaskResult result) {
        if (!StringUtils.hasText(result.videoUrl())) {
            task.setNextPollAt(LocalDateTime.now().plusSeconds(10));
            videoTaskMapper.updateById(task);
            return;
        }
        task.setProviderResultUrl(result.videoUrl());
        log.info("Seedance 原始视频地址: taskId={}, providerTaskId={}, videoUrl={}",
                task.getId(), task.getProviderTaskId(), result.videoUrl());
        task.setCompletionTokens(result.completionTokens());
        task.setTotalTokens(result.totalTokens());
        if (result.completionTokens() != null) {
            task.setEstimatedCostCny(pricingService.estimatedCost(
                    result.completionTokens(), task.getUnitPriceCnyPerMillion()));
        }
        task.setStatus(VideoTaskStatus.DOWNLOADING.name());
        task.setProgress(90);
        task.setNextPollAt(LocalDateTime.now());
        videoTaskMapper.updateById(task);
        publish(task);
        finishDownload(task);
    }

    private void finishDownload(VideoTask task) {
        if (!StringUtils.hasText(task.getProviderResultUrl())) {
            fail(task, "视频生成成功但缺少下载地址");
            return;
        }
        try {
            if ("MOCK".equalsIgnoreCase(task.getProvider())) {
                task.setResultUrl(task.getProviderResultUrl());
            } else {
                String storageKey = videoAssetService.store(
                        task.getUserId(), task.getId(), task.getProviderTaskId(),
                        task.getProviderResultUrl());
                task.setStorageKey(storageKey);
            }
            task.setStatus(VideoTaskStatus.SUCCESS.name());
            task.setProgress(100);
            task.setCompletedAt(LocalDateTime.now());
            releaseLease(task);
            task.setNextPollAt(null);
            task.setErrorMessage(null);
            videoTaskMapper.updateById(task);
            publish(task);
        } catch (Exception ex) {
            int attempts = task.getAttemptCount() == null ? 1 : task.getAttemptCount() + 1;
            task.setAttemptCount(attempts);
            task.setStatus(VideoTaskStatus.DOWNLOADING.name());
            task.setProgress(90);
            task.setErrorMessage("视频已生成，正在重试保存");
            releaseLease(task);
            task.setNextPollAt(LocalDateTime.now().plusSeconds(Math.min(120, 10L * attempts)));
            videoTaskMapper.updateById(task);
            publish(task);
            log.warn("视频转存失败: taskId={}, attempt={}, error={}",
                    task.getId(), attempts, ex.getMessage());
        }
    }

    private void handleWorkerFailure(VideoTask task, Exception error) {
        if (error instanceof VideoProviderException providerError) {
            if (task.getStatus().equals(VideoTaskStatus.PENDING.name())
                    || task.getStatus().equals(VideoTaskStatus.SUBMITTING.name())) {
                int attempts = task.getAttemptCount() == null ? 0 : task.getAttemptCount();
                if (providerError.isRetryable() && attempts < seedanceProperties.getMaxSubmitAttempts()) {
                    task.setStatus(VideoTaskStatus.PENDING.name());
                    task.setAttemptCount(attempts + 1);
                    task.setErrorMessage(providerError.getMessage());
                    releaseLease(task);
                    task.setNextPollAt(LocalDateTime.now().plusSeconds(
                            providerError.getHttpStatus() != null && providerError.getHttpStatus() == 429
                                    ? 60 : Math.min(30, 5L * (attempts + 1))));
                    videoTaskMapper.updateById(task);
                    publish(task);
                    return;
                }
            } else if (providerError.isRetryable()) {
                task.setAttemptCount((task.getAttemptCount() == null ? 0 : task.getAttemptCount()) + 1);
                task.setErrorMessage(providerError.getMessage());
                releaseLease(task);
                task.setNextPollAt(LocalDateTime.now().plusSeconds(15));
                videoTaskMapper.updateById(task);
                return;
            }
            task.setProviderErrorCode(providerError.getProviderCode());
            fail(task, providerError.getMessage());
            return;
        }
        fail(task, "视频任务处理失败，请稍后重试");
    }

    private void fail(VideoTask task, String message) {
        finish(task, VideoTaskStatus.FAILED, task.getProgress(), message);
    }

    private void finish(VideoTask task, VideoTaskStatus status, int progress, String errorMessage) {
        task.setStatus(status.name());
        task.setProgress(progress);
        task.setErrorMessage(errorMessage);
        releaseLease(task);
        task.setNextPollAt(null);
        task.setCompletedAt(LocalDateTime.now());
        videoTaskMapper.updateById(task);
        publish(task);
    }

    private LocalDateTime nextPollTime() {
        String mode = seedanceProperties.getStatusSyncMode().toLowerCase(Locale.ROOT);
        if ("callback".equals(mode)) {
            return LocalDateTime.now().plusMinutes(30);
        }
        if ("hybrid".equals(mode) && providerRouter.callbackConfigured()) {
            return LocalDateTime.now().plus(seedanceProperties.getCallbackFallbackInterval());
        }
        long baseSeconds = Math.max(1, seedanceProperties.getPollInterval().toSeconds());
        long delaySeconds = Math.min(30, baseSeconds);
        return LocalDateTime.now().plusSeconds(delaySeconds);
    }

    private String mappedStatus(String providerStatus) {
        String status = providerStatus == null ? "queued" : providerStatus.toLowerCase(Locale.ROOT);
        return switch (status) {
            case "succeeded" -> VideoTaskStatus.DOWNLOADING.name();
            case "failed" -> VideoTaskStatus.FAILED.name();
            case "cancelled" -> VideoTaskStatus.CANCELLED.name();
            case "expired" -> VideoTaskStatus.EXPIRED.name();
            case "running" -> VideoTaskStatus.RUNNING.name();
            default -> VideoTaskStatus.QUEUED.name();
        };
    }

    private String providerErrorMessage(ProviderTaskResult result) {
        if (StringUtils.hasText(result.errorMessage())) {
            return result.errorMessage();
        }
        return StringUtils.hasText(result.errorCode())
                ? "视频生成失败：" + result.errorCode()
                : "视频生成失败";
    }

    private VideoGenerationParams readParams(String params) {
        if (!StringUtils.hasText(params)) {
            return VideoGenerationParams.defaults();
        }
        try {
            return objectMapper.readValue(params, VideoGenerationParams.class);
        } catch (Exception ex) {
            throw new IllegalStateException("视频参数读取失败", ex);
        }
    }

    private String composePrompt(String prompt, VideoGenerationParams params) {
        String styleValue = params.style();
        if (!StringUtils.hasText(styleValue)) {
            return prompt;
        }
        String style = switch (styleValue) {
            case "cinematic" -> "电影感";
            case "realistic" -> "写实";
            case "anime" -> "动漫";
            case "cyberpunk" -> "赛博朋克";
            case "ink" -> "水墨";
            default -> styleValue;
        };
        return "视觉风格：" + style + "。\n" + prompt;
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

    private void releaseLease(VideoTask task) {
        // MyBatis-Plus 默认忽略 null 更新，写一个过期时间才能实际释放租约。
        task.setLeaseUntil(LocalDateTime.now().minusSeconds(1));
    }
}
