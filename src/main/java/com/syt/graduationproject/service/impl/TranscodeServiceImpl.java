package com.syt.graduationproject.service.impl;

import com.syt.graduationproject.enums.VideoResolutionEnum;
import com.syt.graduationproject.enums.VideoStatusEnum;
import com.syt.graduationproject.exception.CustomException;
import com.syt.graduationproject.model.bo.VideoSourceBo;
import com.syt.graduationproject.model.po.VideoPo;
import com.syt.graduationproject.model.po.VideoSourcePo;
import com.syt.graduationproject.repository.VideoRepository;
import com.syt.graduationproject.service.ManagerService;
import com.syt.graduationproject.service.TranscodeService;
import com.syt.graduationproject.service.minio.MinioService;
import com.syt.graduationproject.util.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranscodeServiceImpl implements TranscodeService {

    private final VideoRepository videoRepository;

    private final MinioService minioService;

    private final StringRedisTemplate stringRedisTemplate;

    private final ManagerService managerService;

    @Override
    public void processVideo(Long videoId, Long userId) {
        log.info("开始处理视频转码任务，videoId:{}, userId:{}", videoId, userId);
        String lockKey = RedisKeyUtil.videoTranscodeLockKey(videoId);
        Boolean lockOk = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 1, TimeUnit.HOURS);
        if (!Boolean.TRUE.equals(lockOk)) {
            log.warn("视频转码任务重复触发，videoId:{}", videoId);
            return;
        }

        Path sourcePath = null;
        Path outputDir = null;
        try {
            VideoPo videoPo = videoRepository.queryVideoById(videoId);
            if (videoPo == null) {
                throw new CustomException("视频不存在");
            }
            if (VideoStatusEnum.WAITING_TRANSCODE.getCode() != videoPo.getStatus()
                    && VideoStatusEnum.TRANSCODE_FAILED.getCode() != videoPo.getStatus()) {
                log.warn("当前状态无需转码，videoId:{}, status:{}", videoId, videoPo.getStatus());
                return;
            }
            log.info("视频状态符合转码条件，开始转码，videoId:{}", videoId);

            videoRepository.updateVideoStatus(videoId, videoPo.getStatus(), VideoStatusEnum.TRANSCODING.getCode());
            log.info("视频状态更新为转码中，videoId:{}", videoId);

            List<VideoSourceBo> sourceBos = videoRepository.queryVideoSource(videoId, VideoResolutionEnum.ORIGINAL.getCode());
            if (sourceBos == null || sourceBos.isEmpty()) {
                throw new CustomException("原视频资源不存在");
            }
            String sourceObjectName = sourceBos.get(0).getPlayUrl();
            log.info("获取到原视频资源，sourceObjectName:{}", sourceObjectName);

            sourcePath = minioService.downloadObjectToTempFile(sourceObjectName, ".mp4");
            outputDir = Files.createTempDirectory("video-hls-" + videoId + "-");
            log.info("下载原视频资源完成，开始转码，sourcePath:{}", sourcePath);

            Path hls720 = outputDir.resolve("720p.m3u8");
            Path hls1080 = outputDir.resolve("1080p.m3u8");

            long start720 = System.currentTimeMillis();
            log.info("开始转码为720p，videoId:{}", videoId);
            transcodeToHls(sourcePath, hls720, 720);
            long end720 = System.currentTimeMillis();
            log.info("720p转码完成，耗时: {}s", (end720 - start720) / 1000);

            long start1080 = System.currentTimeMillis();
            log.info("开始转码为1080p，videoId:{}", videoId);
            transcodeToHls(sourcePath, hls1080, 1080);
            long end1080 = System.currentTimeMillis();
            log.info("1080p转码完成，耗时: {}s", (end1080 - start1080) / 1000);

            Path master = outputDir.resolve("master.m3u8");
            writeMasterPlaylist(master);
            log.info("生成master.m3u8完成，videoId:{}", videoId);

            String objectPrefix = "video/hls/" + videoId + "/" + System.currentTimeMillis();
            minioService.uploadDirectory(outputDir, objectPrefix);

            savePlayableSource(videoId, objectPrefix);
            log.info("保存转码后的视频资源信息完成，videoId:{}", videoId);

            int updated = videoRepository.updateVideoStatus(videoId, VideoStatusEnum.TRANSCODING.getCode(), VideoStatusEnum.AUDITING.getCode());
            if (updated > 0) {
                managerService.createAuditingRecord(videoId, userId);
                log.info("视频转码任务完成，创建对应审核记录，状态更新为待审核，videoId:{}", videoId);
            } else {
                log.warn("视频转码任务完成，状态更新失败，videoId:{}", videoId);
            }
        } catch (Exception e) {
            log.error("视频转码失败，videoId:{}", videoId, e);
            videoRepository.updateVideoStatus(videoId, VideoStatusEnum.TRANSCODING.getCode(), VideoStatusEnum.TRANSCODE_FAILED.getCode());
        } finally {
            cleanup(sourcePath);
            cleanup(outputDir);
            stringRedisTemplate.delete(lockKey);
        }
    }

    private void savePlayableSource(Long videoId, String objectPrefix) {
        for (VideoResolutionEnum videoResolutionEnum : VideoResolutionEnum.values()) {
            if (videoResolutionEnum.equals(VideoResolutionEnum.ORIGINAL)) {
                continue;
            }
            Integer resolutionCode = videoResolutionEnum.getCode();
            String playUrl = String.format("%s/%s.m3u8", objectPrefix, videoResolutionEnum.getResolution());
            videoRepository.deleteVideoSource(videoId, resolutionCode);
            VideoSourcePo sourcePo = VideoSourcePo.builder()
                    .videoId(videoId)
                    .resolution(resolutionCode)
                    .playUrl(playUrl)
                    .format("m3u8")
                    .codec("h264")
                    .build();
            videoRepository.saveVideoSource(sourcePo);
        }
    }

    private void transcodeToHls(Path input, Path outputM3u8, int height) throws IOException, InterruptedException {
        List<String> command = getFFmpegCommand(input, outputM3u8, height);

        log.info("执行FFmpeg命令: {}", String.join(" ", command));
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        AtomicReference<String> lastLine = new AtomicReference<>("");
        Thread outputReader = getOutputReader(height, process, lastLine);

        boolean finished = process.waitFor(30, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new CustomException("FFmpeg转码超时，可能发生阻塞");
        }

        outputReader.join(3000);
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new CustomException("FFmpeg转码失败(exitCode=" + exitCode + "), lastOutput=" + lastLine.get());
        }
    }

    private static Thread getOutputReader(int height, Process process, AtomicReference<String> lastLine) {
        Thread outputReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lastLine.set(line);
                    if (line.contains("time=") || line.contains("frame=")) {
                        log.info("ffmpeg[{}p]: {}", height, line);
                    }
                }
            } catch (IOException e) {
                log.warn("读取FFmpeg输出失败", e);
            }
        }, "ffmpeg-log-reader-" + height);
        outputReader.setDaemon(true);
        outputReader.start();
        return outputReader;
    }

    private static List<String> getFFmpegCommand(Path input, Path outputM3u8, int height) {
        String prefix = outputM3u8.getFileName().toString().replace(".m3u8", "");
        Path segmentPath = outputM3u8.getParent().resolve(prefix + "_%03d.ts");
        return Arrays.asList(
                "ffmpeg", "-y",
                "-i", input.toString(),
                "-vf", "scale=-2:" + height,
                "-c:v", "libx264",
                "-c:a", "aac",
                "-ar", "48000",
                "-g", "48",
                "-keyint_min", "48",
                "-hls_time", "6",
                "-hls_playlist_type", "vod",
                "-hls_segment_filename", segmentPath.toString(),
                outputM3u8.toString()
        );
    }

    private void writeMasterPlaylist(Path masterPath) throws IOException {
        String content = "#EXTM3U\n"
                + "#EXT-X-VERSION:3\n"
                + "#EXT-X-STREAM-INF:BANDWIDTH=2500000,RESOLUTION=1280x720\n"
                + "720p.m3u8\n"
                + "#EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1920x1080\n"
                + "1080p.m3u8\n";
        Files.write(masterPath, content.getBytes(StandardCharsets.UTF_8));
    }

    private void cleanup(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try {
            if (Files.isDirectory(path)) {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @NotNull
                    @Override
                    public FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                        deleteWithRetry(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @NotNull
                    @Override
                    public FileVisitResult postVisitDirectory(@NotNull Path dir, IOException exc) throws IOException {
                        if (exc != null) {
                            throw exc;
                        }
                        deleteWithRetry(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                deleteWithRetry(path);
            }
        } catch (IOException e) {
            log.warn("清理临时路径失败: {}", path, e);
        }
    }

    private void deleteWithRetry(Path path) throws IOException {
        IOException last = null;
        for (int i = 0; i < 3; i++) {
            try {
                Files.deleteIfExists(path);
                return;
            } catch (IOException e) {
                last = e;
                try {
                    Thread.sleep(100L * (i + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("清理临时文件被中断: " + path, ie);
                }
            }
        }
        throw last;
    }
}


