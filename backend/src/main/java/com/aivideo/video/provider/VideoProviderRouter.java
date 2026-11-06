package com.aivideo.video.provider;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class VideoProviderRouter {

    private final Map<String, VideoGenerationProvider> providers;
    private final SeedanceVideoProperties seedanceProperties;
    private final String providerName;

    public VideoProviderRouter(List<VideoGenerationProvider> providers,
                               SeedanceVideoProperties seedanceProperties,
                               @Value("${app.video.provider:mock}") String providerName) {
        this.providers = providers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        provider -> provider.name().toLowerCase(Locale.ROOT),
                        Function.identity()));
        this.seedanceProperties = seedanceProperties;
        this.providerName = providerName.toLowerCase(Locale.ROOT);
    }

    @PostConstruct
    public void validateConfiguration() {
        if (!providers.containsKey(providerName)) {
            throw new IllegalStateException("未知的视频生成服务: " + providerName);
        }
        log.info("视频生成服务: provider={}, model={}, statusSyncMode={}",
                providerName,
                model(),
                seedanceProperties.getStatusSyncMode());
        if (!"seedance".equals(providerName)) {
            return;
        }
        if (!StringUtils.hasText(seedanceProperties.getApiKey())) {
            throw new IllegalStateException("启用 Seedance 时必须设置 ARK_API_KEY");
        }
        String mode = seedanceProperties.getStatusSyncMode().toLowerCase(Locale.ROOT);
        if (!List.of("polling", "callback", "hybrid").contains(mode)) {
            throw new IllegalStateException("SEEDANCE_STATUS_SYNC_MODE 仅支持 polling、callback、hybrid");
        }
        if ("callback".equals(mode)
                && (!StringUtils.hasText(seedanceProperties.getCallbackBaseUrl())
                || !StringUtils.hasText(seedanceProperties.getCallbackSecret()))) {
            throw new IllegalStateException("callback 模式必须配置回调地址和签名密钥");
        }
        if ("hybrid".equals(mode)
                && (!StringUtils.hasText(seedanceProperties.getCallbackBaseUrl())
                || !StringUtils.hasText(seedanceProperties.getCallbackSecret()))) {
            log.warn("hybrid 模式未配置完整回调参数，将退回轮询模式");
        }
    }

    public VideoGenerationProvider current() {
        return providers.get(providerName);
    }

    public String providerName() {
        return providerName;
    }

    public String model() {
        return "seedance".equals(providerName) ? seedanceProperties.getModel() : "mock-seedance";
    }

    public boolean callbackEnabled() {
        return "seedance".equals(providerName) && isCallbackConfigured();
    }

    public boolean callbackConfigured() {
        return isCallbackConfigured();
    }

    private boolean isCallbackConfigured() {
        String mode = seedanceProperties.getStatusSyncMode().toLowerCase(Locale.ROOT);
        return ("callback".equals(mode) || "hybrid".equals(mode))
                && StringUtils.hasText(seedanceProperties.getCallbackBaseUrl())
                && StringUtils.hasText(seedanceProperties.getCallbackSecret());
    }
}
