package com.aivideo.storage;

import java.time.Duration;

/**
 * 对象存储抽象。后续可替换为 MinIO / AWS S3 / 阿里云 OSS 实现。
 */
public interface ObjectStorageService {

    /**
     * 上传对象并返回可访问 URL。
     * TODO: 当前为占位实现，不会真正写入文件。
     */
    String upload(byte[] content, String objectKey, String contentType);

    /**
     * 生成预签名访问 URL。
     */
    String presignedUrl(String objectKey, Duration ttl);
}
