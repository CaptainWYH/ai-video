package com.aivideo.video.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("video_tasks")
public class VideoTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long projectId;
    private Long chatSessionId;
    private Long chatMessageId;
    private String prompt;
    /** JSON 字符串：分辨率、时长、风格等生成参数 */
    private String params;
    /** PENDING / SUBMITTING / QUEUED / RUNNING / DOWNLOADING / SUCCESS / FAILED / CANCELLED / EXPIRED */
    private String status;
    private Integer progress;
    private String resultUrl;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String errorMessage;
    private String provider;
    private String providerTaskId;
    private String providerStatus;
    private String providerModel;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String providerErrorCode;
    private String providerResultUrl;
    private String storageKey;
    private String clientRequestId;
    private Long completionTokens;
    private Long totalTokens;
    private BigDecimal unitPriceCnyPerMillion;
    private String pricingVersion;
    private BigDecimal estimatedCostCny;
    private String currency;
    private LocalDateTime submittedAt;
    private LocalDateTime lastPolledAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime nextPollAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime completedAt;
    private LocalDateTime leaseUntil;
    private Integer attemptCount;

    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
