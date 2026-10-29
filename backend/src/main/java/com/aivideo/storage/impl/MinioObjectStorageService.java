package com.aivideo.storage.impl;

import com.aivideo.storage.ObjectStorageService;
import com.aivideo.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * MinIO/S3 占位实现。
 *
 * <p>TODO: 接入 MinIO Java SDK 或 AWS S3 SDK：
 * <ul>
 *   <li>初始化 MinioClient（endpoint/accessKey/secretKey）；</li>
 *   <li>upload 调用 putObject，并设置 Content-Type；</li>
 *   <li>presignedUrl 调用 getPresignedObjectUrl，设置过期时间。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioObjectStorageService implements ObjectStorageService {

    private final StorageProperties properties;

    @Override
    public String upload(byte[] content, String objectKey, String contentType) {
        // TODO 真实上传逻辑，本阶段仅返回占位 URL，不写文件。
        log.info("对象存储占位调用: bucket={}, key={}, contentType={}, size={}",
                properties.getBucket(), objectKey, contentType,
                content == null ? 0 : content.length);
        return mockUrl(objectKey);
    }

    @Override
    public String presignedUrl(String objectKey, Duration ttl) {
        // TODO 使用 SDK 生成预签名 URL。
        return mockUrl(objectKey) + "?expires=" + ttl.toSeconds();
    }

    private String mockUrl(String objectKey) {
        return properties.getEndpoint() + "/" + properties.getBucket() + "/" + objectKey;
    }
}
