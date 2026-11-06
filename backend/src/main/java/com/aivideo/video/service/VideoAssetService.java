package com.aivideo.video.service;

import com.aivideo.storage.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class VideoAssetService {

    private static final long MAX_VIDEO_BYTES = 2L * 1024 * 1024 * 1024;

    private final ObjectStorageService objectStorageService;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public String store(Long userId, Long taskId, String providerTaskId, String videoUrl) {
        URI uri = URI.create(videoUrl);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !trustedHost(uri.getHost())) {
            throw new IllegalArgumentException("不安全的视频下载地址");
        }

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMinutes(3))
                .GET()
                .build();
        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                closeQuietly(response.body());
                throw new IllegalStateException("下载 Seedance 视频失败: HTTP " + response.statusCode());
            }
            long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1);
            if (contentLength > MAX_VIDEO_BYTES) {
                closeQuietly(response.body());
                throw new IllegalStateException("Seedance 视频超过 2GB 上限");
            }
            String contentType = response.headers().firstValue("Content-Type")
                    .orElse("video/mp4");
            String objectKey = "videos/" + userId + "/" + taskId + "/"
                    + providerTaskId.replaceAll("[^A-Za-z0-9._-]", "_") + extension(contentType);
            try (InputStream input = response.body()) {
                return objectStorageService.upload(input, contentLength, objectKey, contentType);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("下载 Seedance 视频被中断", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("保存 Seedance 视频失败", ex);
        }
    }

    private boolean trustedHost(String host) {
        return host != null && (host.equals("volces.com") || host.endsWith(".volces.com"));
    }

    private String extension(String contentType) {
        return contentType != null && contentType.contains("quicktime") ? ".mov" : ".mp4";
    }

    private void closeQuietly(InputStream stream) {
        try {
            stream.close();
        } catch (Exception ignored) {
            // 清理下载响应时忽略关闭异常。
        }
    }
}
