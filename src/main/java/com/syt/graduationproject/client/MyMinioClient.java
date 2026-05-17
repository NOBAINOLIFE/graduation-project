package com.syt.graduationproject.client;

import com.syt.graduationproject.exception.CustomException;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MyMinioClient {

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
        String objectName = folder + hashName;
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
     * 上传分片文件
     */
    public void uploadPartFile(MultipartFile file, String objectName) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        } catch (Exception e) {
            log.error("上传分片失败，objectName:{}", objectName, e);
            throw new CustomException("上传分片失败");
        }
    }

    /**
     * 合并分片对象
     */
    public void composeObject(List<String> partObjectNames, String targetObjectName) {
        try {
            List<ComposeSource> sources = new ArrayList<>();
            for (String part : partObjectNames) {
                sources.add(ComposeSource.builder()
                        .bucket(bucketName)
                        .object(part)
                        .build());
            }
            minioClient.composeObject(
                    ComposeObjectArgs.builder()
                            .bucket(bucketName)
                            .object(targetObjectName)
                            .sources(sources)
                            .build()
            );
        } catch (Exception e) {
            log.error("合并分片失败，targetObjectName:{}", targetObjectName, e);
            throw new CustomException("合并分片失败");
        }
    }

    /**
     * 下载对象到本地临时文件
     */
    public Path downloadObjectToTempFile(String objectName, String suffix) {
        try {
            Path tempFile = Files.createTempFile("video-src-", suffix);
            try (InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder().bucket(bucketName).object(objectName).build())) {
                Files.copy(inputStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return tempFile;
        } catch (Exception e) {
            log.error("下载对象失败，objectName:{}", objectName, e);
            throw new CustomException("下载源视频失败");
        }
    }

    /**
     * 上传本地文件
     */
    public void uploadLocalFile(Path localPath, String objectName, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(Files.newInputStream(localPath), Files.size(localPath), -1)
                            .contentType(contentType)
                            .build()
            );
        } catch (Exception e) {
            log.error("上传本地文件失败，objectName:{}", objectName, e);
            throw new CustomException("上传转码文件失败");
        }
    }

    /**
     * 上传目录中的所有文件（相对路径保持不变）
     */
    public void uploadDirectory(Path directory, String objectPrefix) {
        try {
            Files.walkFileTree(directory, new FileVisitor<Path>() {
                @NotNull
                @Override
                public FileVisitResult preVisitDirectory(Path dir, @NotNull BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE;
                }

                @NotNull
                @Override
                public FileVisitResult visitFile(Path file, @NotNull BasicFileAttributes attrs) {
                    String relative = directory.relativize(file).toString().replace('\\', '/');
                    String objectName = objectPrefix + "/" + relative;
                    String contentType = guessContentType(file);
                    uploadLocalFile(file, objectName, contentType);
                    return FileVisitResult.CONTINUE;
                }

                @NotNull
                @Override
                public FileVisitResult visitFileFailed(Path file, @NotNull IOException exc) {
                    return FileVisitResult.TERMINATE;
                }

                @NotNull
                @Override
                public FileVisitResult postVisitDirectory(Path dir, java.io.IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            log.error("上传目录失败，directory:{}", directory, e);
            throw new CustomException("上传转码目录失败");
        }
    }

    private String guessContentType(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".m3u8")) {
            return "application/vnd.apple.mpegurl";
        }
        if (name.endsWith(".ts")) {
            return "video/mp2t";
        }
        if (name.endsWith(".mp4")) {
            return "video/mp4";
        }
        return "application/octet-stream";
    }

    /**
     * 生成 GET 预签名 URL（用于播放鉴权）
     */
    public String generateGetUrl(String objectName, int expiryMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(expiryMinutes, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("生成下载链接失败", e);
            throw new CustomException("生成下载链接失败");
        }
    }

    /**
     * 获取文件的完整访问URL
     */
    public String getFileUrl(String objectName) {
        return endpoint + "/" + bucketName + "/" + objectName;
    }
}
