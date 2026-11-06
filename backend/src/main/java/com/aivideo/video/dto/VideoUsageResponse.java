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
public class VideoUsageResponse {

    private Long completionTokens;
    private Long totalTokens;
    private BigDecimal unitPriceCnyPerMillion;
    private String pricingVersion;
    private BigDecimal estimatedCostCny;
    private String currency;
}
