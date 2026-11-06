package com.aivideo.storage.impl;

import com.aivideo.storage.ObjectStorageService;
import com.aivideo.storage.StorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 对象存储实现。视频以流式方式上传，避免将大文件整体载入内存。
 */
@Service
public class MinioObjectStorageService implements ObjectStorageService {

    private static final long PART_SIZE = 10L * 1024 * 1024;

    private final StorageProperties properties;
    private final MinioClient client;
    private volatile boolean bucketReady;

    public MinioObjectStorageService(StorageProperties properties) {
        this.properties = properties;
        if (!StringUtils.hasText(properties.getEndpoint())) {
            throw new IllegalStateException("MinIO endpoint 不能为空");
        }
        this.client = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    @Override
    public String upload(InputStream content, long contentLength, String objectKey, String contentType) {
        ensureBucket();
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .stream(content, contentLength, PART_SIZE)
                    .contentType(contentType)
                    .build());
            return objectKey;
        } catch (Exception ex) {
            throw new IllegalStateException("上传视频到 MinIO 失败: " + objectKey, ex);
        }
    }

    @Override
    public String presignedUrl(String objectKey, Duration ttl) {
        ensureBucket();
        try {
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .expiry((int) Math.min(Integer.MAX_VALUE, Math.max(1, ttl.toSeconds())),
                            TimeUnit.SECONDS)
                    .build());
        } catch (Exception ex) {
            throw new IllegalStateException("生成 MinIO 预签名地址失败: " + objectKey, ex);
        }
    }

    private void ensureBucket() {
        if (bucketReady) {
            return;
        }
        synchronized (this) {
            if (bucketReady) {
                return;
            }
            try {
                boolean exists = client.bucketExists(BucketExistsArgs.builder()
                        .bucket(properties.getBucket())
                        .build());
                if (!exists) {
                    client.makeBucket(MakeBucketArgs.builder()
                            .bucket(properties.getBucket())
                            .build());
                }
                bucketReady = true;
            } catch (Exception ex) {
                throw new IllegalStateException("初始化 MinIO bucket 失败: " + properties.getBucket(), ex);
            }
        }
    }
}
