package com.syt.graduationproject.schedule;

import com.syt.graduationproject.repository.VideoRepository;
import com.syt.graduationproject.util.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 定时将 Redis 中的视频 PV 增量回刷到数据库
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoPvFlushJob {

    private final StringRedisTemplate stringRedisTemplate;

    private final VideoRepository videoRepository;

    /**
     * 每 10 秒刷一次视频 PV 增量
     */
    @Scheduled(fixedDelay = 10_000)
    public void flushPvDelta() {
        String pvDeltaKey = RedisKeyUtil.videoPlayPvDeltaKey();
        Map<Object, Object> deltaMap = stringRedisTemplate.opsForHash().entries(pvDeltaKey);
        if (deltaMap.isEmpty()) {
            return;
        }
        for (Map.Entry<Object, Object> entry : deltaMap.entrySet()) {
            Long videoId = parseLong(entry.getKey());
            Long delta = parseLong(entry.getValue());
            if (videoId == null || delta == null || delta <= 0) {
                continue;
            }
            try {
                int updated = videoRepository.incrVideoPlayCount(videoId, delta);
                if (updated <= 0) {
                    videoRepository.insertVideoStatsIfAbsent(videoId);
                    updated = videoRepository.incrVideoPlayCount(videoId, delta);
                }
                if (updated > 0) {
                    Long remain = stringRedisTemplate.opsForHash().increment(pvDeltaKey, String.valueOf(videoId), -delta);
                    if (remain <= 0) {
                        stringRedisTemplate.opsForHash().delete(pvDeltaKey, String.valueOf(videoId));
                    }
                }
            } catch (Exception e) {
                log.warn("回刷视频PV失败，videoId:{}, delta:{}", videoId, delta, e);
            }
        }
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}


