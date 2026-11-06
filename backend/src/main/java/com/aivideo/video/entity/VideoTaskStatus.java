package com.aivideo.video.entity;

/**
 * 视频任务状态。
 */
public enum VideoTaskStatus {
    PENDING,
    SUBMITTING,
    QUEUED,
    RUNNING,
    DOWNLOADING,
    SUCCESS,
    FAILED,
    CANCELLED,
    EXPIRED
}
