package com.aivideo.video.provider;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "app.video.seedance")
public class SeedanceVideoProperties {

    private String apiKey = "";
    private String baseUrl = "https://ark.cn-beijing.volces.com/api/v3";
    private String model = "doubao-seedance-2-5-260628";
    private Duration requestTimeout = Duration.ofSeconds(15);
    private String statusSyncMode = "polling";
    private Duration pollInterval = Duration.ofSeconds(5);
    private Duration callbackFallbackInterval = Duration.ofMinutes(2);
    private String callbackBaseUrl = "";
    private String callbackSecret = "";
    private int maxSubmitAttempts = 3;
    private int maxBatchSize = 20;
    private Duration workerLease = Duration.ofMinutes(2);
    private String pricingVersion = "seedance-2.5-2026-11";
    private Map<String, Map<String, BigDecimal>> pricing = new LinkedHashMap<>();
}
