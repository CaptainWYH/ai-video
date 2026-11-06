package com.aivideo.video.service;

import com.aivideo.video.provider.SeedanceVideoProperties;
import com.aivideo.video.provider.VideoGenerationOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoPricingService {

    private final SeedanceVideoProperties properties;

    public PriceQuote quote(String model, VideoGenerationOptions options) {
        Map<String, BigDecimal> modelPrices = properties.getPricing().get(model);
        BigDecimal unitPrice = modelPrices == null ? null : modelPrices.get(options.resolution());
        if (unitPrice == null) {
            log.warn("未配置视频计价快照: model={}, resolution={}", model, options.resolution());
        }
        return new PriceQuote(unitPrice, properties.getPricingVersion(), "CNY");
    }

    public BigDecimal estimatedCost(Long completionTokens, BigDecimal unitPrice) {
        if (completionTokens == null || unitPrice == null) {
            return null;
        }
        return BigDecimal.valueOf(completionTokens)
                .multiply(unitPrice)
                .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
    }

    public record PriceQuote(BigDecimal unitPriceCnyPerMillion, String pricingVersion, String currency) {
    }
}
