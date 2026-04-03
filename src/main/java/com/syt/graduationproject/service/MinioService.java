package com.syt.graduationproject.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 服务
 */
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucketName}")
    private String bucketName;

    /**
     * 上传文件
     */
    public String uploadFile(MultipartFile file) throws Exception {
        // 生成唯一文件名，防止覆盖
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
        return fileName;
    }

    /**
     * 生成带时效的查看链接（例如：预览 2 小时）
     */
    public String getPreviewUrl(String fileName) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(fileName)
                        .expiry(2, TimeUnit.HOURS)
                        .build()
        );
    }
}