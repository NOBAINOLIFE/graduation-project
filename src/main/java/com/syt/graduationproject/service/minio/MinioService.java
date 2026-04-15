package com.syt.graduationproject.service.minio;

import com.syt.graduationproject.exception.CustomException;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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

    @Value("${minio.endpoint}")
    private String endpoint;

    /**
     * 上传文件到指定文件夹
     */
    public String uploadFile(MultipartFile file, String folder) throws Exception {
        String hashName = DigestUtils.md5DigestAsHex(file.getBytes());
        String objectName = folder + "/" + hashName;
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
        return getFileUrl(objectName);
    }

    /**
     * 通过输入流上传文件（用于服务端转码等场景）
     *
     * @param objectName MinIO 中的对象相对路径
     * @param inputStream 文件输入流
     * @param size 文件大小
     * @param contentType 内容类型
     * @return 实际保存的对象相对路径
     */
    public String uploadStream(String objectName, InputStream inputStream, long size, String contentType) throws Exception {
        String finalObjectName = objectName;
        if (finalObjectName == null || finalObjectName.isEmpty()) {
            finalObjectName = UUID.randomUUID().toString();
        }
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(finalObjectName)
                        .stream(inputStream, size, -1)
                        .contentType(contentType)
                        .build()
        );
        return finalObjectName;
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

    /**
     * 下载 MinIO 对象到本地文件
     *
     * @param objectName MinIO 中对象相对路径
     * @param targetFile 本地目标文件路径
     */
    public void downloadToFile(String objectName, Path targetFile) throws Exception {
        if (objectName == null || objectName.isEmpty()) {
            throw new IllegalArgumentException("objectName 不能为空");
        }
        if (targetFile == null) {
            throw new IllegalArgumentException("targetFile 不能为空");
        }

        Files.createDirectories(targetFile.getParent());
        try (InputStream in = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        ); OutputStream out = Files.newOutputStream(targetFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }
    }

    /**
     * 获取文件的完整访问URL
     */
    public String getFileUrl(String objectName) {
        return endpoint + "/" + bucketName + "/" + objectName;
    }

}
