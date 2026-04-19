package com.syt.graduationproject.service.impl;

import com.syt.graduationproject.enums.VideoResolutionEnum;
import com.syt.graduationproject.enums.VideoStatusEnum;
import com.syt.graduationproject.exception.CustomException;
import com.syt.graduationproject.model.po.VideoPo;
import com.syt.graduationproject.model.po.VideoSourcePo;
import com.syt.graduationproject.repository.VideoRepository;
import com.syt.graduationproject.service.ManagerService;
import com.syt.graduationproject.service.TranscodeService;
import com.syt.graduationproject.service.minio.MinioService;
import com.syt.graduationproject.util.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
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

            videoRepository.updateVideoStatus(videoId, videoPo.getStatus(), VideoStatusEnum.TRANSCODING.getCode());

            List<VideoSourcePo> sourcePos = videoRepository.queryVideoSourcePos(videoId, VideoResolutionEnum.ORIGINAL.getCode());
            if (sourcePos == null || sourcePos.isEmpty()) {
                throw new CustomException("原视频资源不存在");
            }
            String sourceObjectName = sourcePos.get(0).getPlayUrl();
            sourcePath = minioService.downloadObjectToTempFile(sourceObjectName, ".mp4");
            outputDir = Files.createTempDirectory("video-hls-" + videoId + "-");

            Path hls720 = outputDir.resolve("720p.m3u8");
            Path hls1080 = outputDir.resolve("1080p.m3u8");
            transcodeToHls(sourcePath, hls720, 720);
            transcodeToHls(sourcePath, hls1080, 1080);

            Path master = outputDir.resolve("master.m3u8");
            writeMasterPlaylist(master);

            String objectPrefix = "video/hls/" + videoId + "/" + System.currentTimeMillis();
            minioService.uploadDirectory(outputDir, objectPrefix);

            savePlayableSource(videoId, VideoResolutionEnum.HIGH.getCode(), objectPrefix + "/720p.m3u8");
            savePlayableSource(videoId, VideoResolutionEnum.SUPER.getCode(), objectPrefix + "/1080p.m3u8");
            savePlayableSource(videoId, VideoResolutionEnum.MASTER.getCode(), objectPrefix + "/master.m3u8");

            int updated = videoRepository.updateVideoStatus(videoId, VideoStatusEnum.TRANSCODING.getCode(), VideoStatusEnum.AUDITING.getCode());
            if (updated > 0) {
                managerService.createAuditingRecord(videoId, userId);
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

    private void savePlayableSource(Long videoId, Integer resolution, String playUrl) {
        videoRepository.deleteVideoSource(videoId, resolution);
        VideoSourcePo sourcePo = VideoSourcePo.builder()
                .videoId(videoId)
                .resolution(resolution)
                .playUrl(playUrl)
                .format("m3u8")
                .codec("h264")
                .status(1)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        videoRepository.saveVideoSource(sourcePo);
    }

    private void transcodeToHls(Path input, Path outputM3u8, int height) throws IOException, InterruptedException {
        String prefix = outputM3u8.getFileName().toString().replace(".m3u8", "");
        Path segmentPath = outputM3u8.getParent().resolve(prefix + "_%03d.ts");
        List<String> command = Arrays.asList(
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
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new CustomException("FFmpeg转码失败，请检查FFmpeg是否可用");
        }
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
                Files.walk(path)
                        .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException e) {
                                log.warn("清理临时文件失败: {}", p, e);
                            }
                        });
            } else {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            log.warn("清理临时路径失败: {}", path, e);
        }
    }
}


