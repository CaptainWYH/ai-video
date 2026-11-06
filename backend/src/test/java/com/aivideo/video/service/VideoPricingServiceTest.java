package com.aivideo.video.service;

import com.aivideo.video.provider.SeedanceVideoProperties;
import com.aivideo.video.provider.VideoGenerationOptions;
import com.aivideo.video.dto.VideoGenerationParams;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VideoPricingServiceTest {

    private static final String MODEL = "doubao-seedance-2-5-260628";

    @Test
    void calculates720pEstimatedCostFromCompletionTokens() {
        VideoPricingService.PriceQuote quote = quote("720p", "70.00");

        BigDecimal cost = new VideoPricingService(properties("720p", "70.00"))
                .estimatedCost(108_000L, quote.unitPriceCnyPerMillion());

        assertEquals(new BigDecimal("7.560000"), cost);
    }

    @Test
    void uses1080pPriceSnapshot() {
        VideoPricingService.PriceQuote quote = quote("1080p", "77.00");

        assertEquals(new BigDecimal("77.00"), quote.unitPriceCnyPerMillion());
        assertEquals("test-pricing", quote.pricingVersion());
    }

    private VideoPricingService.PriceQuote quote(String resolution, String price) {
        return new VideoPricingService(properties(resolution, price)).quote(
                MODEL, VideoGenerationOptions.from(new VideoGenerationParams(
                        "cinematic", "16:9", 5, resolution, true, false, "mp4")));
    }

    private SeedanceVideoProperties properties(String resolution, String price) {
        SeedanceVideoProperties properties = new SeedanceVideoProperties();
        properties.setPricingVersion("test-pricing");
        properties.setPricing(Map.of(MODEL, Map.of(resolution, new BigDecimal(price))));
        return properties;
    }
}
