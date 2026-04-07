package com.syt.graduationproject.service;

import com.syt.graduationproject.exception.CustomException;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 服务
 */
@Slf4j
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
     * 生成带时效的查看链接（例如：预览 15 分钟）
     */
    public String generateUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(15, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("生成预览链接失败", e);
            throw new CustomException("生成预览链接失败");
        }
    }

    /**
     * 检查文件是否存在
     */
    public boolean checkFileExists(String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("404") || errorMsg.contains("Not Found")
                    || errorMsg.contains("NoSuchKey"))) {
                log.warn("文件不存在，objectName: {}", objectName);
                return false;
            }
            log.error("检查文件存在性失败，objectName: {}, error: {}", objectName, errorMsg, e);
            throw new CustomException("检查文件状态失败");
        }
    }

}