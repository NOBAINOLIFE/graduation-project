package com.syt.graduationproject.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.syt.graduationproject.enums.VideoResolutionEnum;
import com.syt.graduationproject.enums.VideoTranscodingStatusEnum;
import com.syt.graduationproject.model.dto.VideoTranscodingDto;
import com.syt.graduationproject.model.po.VideoSourcePo;
import com.syt.graduationproject.mapper.VideoSourceMapper;
import com.syt.graduationproject.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static com.syt.graduationproject.config.KafkaConfig.VIDEO_PROCESS_TOPIC;

/**
 * 视频处理 Kafka 消费者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoProcessConsumer {

    private final MinioService minioService;

    private final VideoSourceMapper videoSourceMapper;

    /**
     * 接收视频转码任务
     */
    @KafkaListener(topics = VIDEO_PROCESS_TOPIC, groupId = "video-process-group")
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(VideoTranscodingDto dto) {
        if (dto == null || dto.getVideoId() == null || StringUtils.isBlank(dto.getOriginalResolutionUrl())) {
            log.warn("收到无效的视频转码消息: {}", dto);
            return;
        }

        Long videoId = dto.getVideoId();
        String originalUrl = dto.getOriginalResolutionUrl();
        log.info("接收到视频转码消息，videoId: {}, originalUrl: {}", videoId, originalUrl);

        // 1. 准备转码任务：480p、720p、1080p
        Map<Integer, VideoResolutionEnum> heightToEnum = new LinkedHashMap<>();
        heightToEnum.put(480, VideoResolutionEnum.MEDIUM);
        heightToEnum.put(720, VideoResolutionEnum.HIGH);
        heightToEnum.put(1080, VideoResolutionEnum.SUPER);

        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("video-transcode-");
        } catch (Exception e) {
            log.error("创建临时目录失败", e);
            return;
        }

        String sourceSuffix = getSuffixFromObjectName(originalUrl, ".mp4");
        File sourceFile = tempDir.resolve("source-" + UUID.randomUUID() + sourceSuffix).toFile();

        try {
            // 2. 从 MinIO 下载原画视频到本地临时文件
            minioService.downloadToFile(originalUrl, sourceFile.toPath());

            // 3. 将原画记录状态改为“转码成功”
            updateOriginalSourceStatusSuccess(videoId);

            // 4. 依次转码并上传（480p/720p/1080p）
            for (Map.Entry<Integer, VideoResolutionEnum> entry : heightToEnum.entrySet()) {
                Integer height = entry.getKey();
                VideoResolutionEnum resolutionEnum = entry.getValue();

                String targetObject = buildTargetObjectPath(originalUrl, "-" + height + "p", ".mp4");
                File targetFile = tempDir.resolve("target-" + height + "p-" + UUID.randomUUID() + ".mp4").toFile();

                try {
                    // 4.1 写入一条“转码中”的记录（可选：如果已存在同分辨率记录，可先删除或更新）
                    insertOrUpdateTranscoding(videoId, resolutionEnum.getCode(), targetObject);

                    // 4.2 转码
                    transcodeWithFfmpeg(sourceFile, targetFile, height);

                    // 4.3 上传
                    String objectName;
                    Path path = targetFile.toPath();
                    try (InputStream in = Files.newInputStream(path)) {
                        objectName = minioService.uploadStream(targetObject, in, targetFile.length(), "video/mp4");
                    }

                    // 4.4 更新该清晰度记录为成功 + 写入元信息
                    updateTranscodingSuccess(videoId, resolutionEnum.getCode(), objectName, targetFile.length());
                    log.info("转码并上传成功，videoId: {}, height: {}, objectName: {}", videoId, height, objectName);

                } catch (Exception e) {
                    log.error("转码或上传失败，videoId: {}, height: {}", videoId, height, e);
                    updateTranscodingFail(videoId, resolutionEnum.getCode());
                } finally {
                    try {
                        Files.deleteIfExists(targetFile.toPath());
                    } catch (Exception ignore) {
                    }
                }
            }

        } catch (Exception e) {
            log.error("处理视频转码任务失败，videoId: {}", videoId, e);
        } finally {
            // 清理临时文件
            try {
                Files.deleteIfExists(sourceFile.toPath());
                Files.deleteIfExists(tempDir);
            } catch (Exception e) {
                log.warn("清理临时文件失败", e);
            }
        }
    }

    /**
     * 构造转码后文件在 MinIO 中的相对路径
     */
    private String buildTargetObjectPath(String originalUrl, String nameSuffix, String fileSuffix) {
        String basePath = originalUrl;
        int dotIndex = basePath.lastIndexOf('.');
        if (dotIndex > 0) {
            basePath = basePath.substring(0, dotIndex);
        }
        return basePath + nameSuffix + fileSuffix;
    }

    private String getSuffixFromObjectName(String objectName, String defaultSuffix) {
        if (StringUtils.isBlank(objectName)) {
            return defaultSuffix;
        }
        int dotIndex = objectName.lastIndexOf('.');
        if (dotIndex > -1 && dotIndex < objectName.length() - 1) {
            return objectName.substring(dotIndex);
        }
        return defaultSuffix;
    }

    /**
     * 使用 FFmpeg 将原始视频转为 H.264 指定高度 MP4
     */
    private void transcodeWithFfmpeg(File sourceFile, File targetFile, int height) throws Exception {
        String[] cmd = new String[]{
                "ffmpeg",
                "-y",
                "-i", sourceFile.getAbsolutePath(),
                "-vf", "scale=-2:" + height,
                "-c:v", "libx264",
                "-preset", "fast",
                "-crf", "23",
                "-c:a", "aac",
                "-b:a", "128k",
                targetFile.getAbsolutePath()
        };
        log.info("开始执行 FFmpeg 转码命令，height: {}", height);
        Process process = new ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg 转码失败，exitCode=" + exitCode);
        }
    }

    private void updateOriginalSourceStatusSuccess(Long videoId) {
        VideoSourcePo update = VideoSourcePo.builder()
                .status(VideoTranscodingStatusEnum.TRANSCODING_SUCCESS.getCode())
                .updateTime(LocalDateTime.now())
                .build();
        videoSourceMapper.update(
                update,
                new LambdaQueryWrapper<VideoSourcePo>()
                        .eq(VideoSourcePo::getVideoId, videoId)
                        .eq(VideoSourcePo::getResolution, VideoResolutionEnum.ORIGINAL.getCode())
        );
    }

    private void insertOrUpdateTranscoding(Long videoId, Integer resolutionCode, String targetObject) {
        // 若已存在同 videoId+resolution 记录，更新为“转码中”；否则插入一条
        VideoSourcePo exists = videoSourceMapper.selectOne(
                new LambdaQueryWrapper<VideoSourcePo>()
                        .eq(VideoSourcePo::getVideoId, videoId)
                        .eq(VideoSourcePo::getResolution, resolutionCode)
                        .last("limit 1")
        );
        if (exists == null) {
            VideoSourcePo insert = VideoSourcePo.builder()
                    .videoId(videoId)
                    .resolution(resolutionCode)
                    .playUrl(targetObject)
                    .format("mp4")
                    .codec("h264")
                    .status(VideoTranscodingStatusEnum.TRANSCODING.getCode())
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            videoSourceMapper.insert(insert);
            return;
        }
        VideoSourcePo update = VideoSourcePo.builder()
                .playUrl(targetObject)
                .format("mp4")
                .codec("h264")
                .status(VideoTranscodingStatusEnum.TRANSCODING.getCode())
                .updateTime(LocalDateTime.now())
                .build();
        videoSourceMapper.update(
                update,
                new LambdaQueryWrapper<VideoSourcePo>()
                        .eq(VideoSourcePo::getId, exists.getId())
        );
    }

    private void updateTranscodingSuccess(Long videoId, Integer resolutionCode, String objectName, long fileSize) {
        VideoSourcePo update = VideoSourcePo.builder()
                .playUrl(objectName)
                .format("mp4")
                .codec("h264")
                .fileSize(fileSize)
                .status(VideoTranscodingStatusEnum.TRANSCODING_SUCCESS.getCode())
                .updateTime(LocalDateTime.now())
                .build();
        videoSourceMapper.update(
                update,
                new LambdaQueryWrapper<VideoSourcePo>()
                        .eq(VideoSourcePo::getVideoId, videoId)
                        .eq(VideoSourcePo::getResolution, resolutionCode)
        );
    }

    private void updateTranscodingFail(Long videoId, Integer resolutionCode) {
        VideoSourcePo update = VideoSourcePo.builder()
                .status(VideoTranscodingStatusEnum.TRANSCODING_FAIL.getCode())
                .updateTime(LocalDateTime.now())
                .build();
        videoSourceMapper.update(
                update,
                new LambdaQueryWrapper<VideoSourcePo>()
                        .eq(VideoSourcePo::getVideoId, videoId)
                        .eq(VideoSourcePo::getResolution, resolutionCode)
        );
    }
}

