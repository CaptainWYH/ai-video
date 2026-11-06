package com.aivideo.storage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;

/**
 * 对象存储抽象。后续可替换为 MinIO / AWS S3 / 阿里云 OSS 实现。
 */
public interface ObjectStorageService {

    String upload(InputStream content, long contentLength, String objectKey, String contentType);

    default String upload(byte[] content, String objectKey, String contentType) {
        return upload(new ByteArrayInputStream(content), content.length, objectKey, contentType);
    }

    /**
     * 生成预签名访问 URL。
     */
    String presignedUrl(String objectKey, Duration ttl);
}
