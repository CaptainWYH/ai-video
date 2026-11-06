package com.aivideo.video.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoUsageSummaryResponse {

    private long taskCount;
    private long successCount;
    private long completionTokens;
    private BigDecimal estimatedCostCny;
    private String currency;
}
