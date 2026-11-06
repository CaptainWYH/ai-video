package com.aivideo.video.controller;

import com.aivideo.common.PageResult;
import com.aivideo.common.Result;
import com.aivideo.security.SecurityUtils;
import com.aivideo.video.dto.CreateVideoTaskRequest;
import com.aivideo.video.dto.VideoTaskResponse;
import com.aivideo.video.dto.VideoUsageSummaryResponse;
import com.aivideo.video.service.VideoGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "视频生成", description = "Seedance 异步视频任务与消耗统计")
@RestController
@RequestMapping("/api/v1/video")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class VideoController {

    private final VideoGenerationService videoGenerationService;

    @Operation(summary = "创建异步视频生成任务")
    @PostMapping("/tasks")
    public Result<VideoTaskResponse> createTask(@Valid @RequestBody CreateVideoTaskRequest request) {
        return Result.ok(videoGenerationService.createTask(SecurityUtils.currentUserId(), request));
    }

    @Operation(summary = "查询视频任务状态")
    @GetMapping("/tasks/{id}")
    public Result<VideoTaskResponse> getTask(@PathVariable("id") Long taskId) {
        return Result.ok(videoGenerationService.getTask(SecurityUtils.currentUserId(), taskId));
    }

    @Operation(summary = "视频任务列表（分页）")
    @GetMapping("/tasks")
    public Result<PageResult<VideoTaskResponse>> listTasks(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return Result.ok(videoGenerationService.listTasks(SecurityUtils.currentUserId(), page, size));
    }

    @Operation(summary = "取消排队中的视频任务")
    @DeleteMapping("/tasks/{id}")
    public Result<VideoTaskResponse> cancelTask(@PathVariable("id") Long taskId) {
        return Result.ok(videoGenerationService.cancelTask(SecurityUtils.currentUserId(), taskId));
    }

    @Operation(summary = "重新创建视频任务")
    @PostMapping("/tasks/{id}/retry")
    public Result<VideoTaskResponse> retryTask(@PathVariable("id") Long taskId) {
        return Result.ok(videoGenerationService.retryTask(SecurityUtils.currentUserId(), taskId));
    }

    @Operation(summary = "视频生成消耗汇总（预估金额）")
    @GetMapping("/usage/summary")
    public Result<VideoUsageSummaryResponse> usageSummary(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return Result.ok(videoGenerationService.usageSummary(
                SecurityUtils.currentUserId(), from, to));
    }
}
