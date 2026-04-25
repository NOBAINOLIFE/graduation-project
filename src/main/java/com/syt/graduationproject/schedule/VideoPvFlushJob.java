package com.syt.graduationproject.schedule;

import com.syt.graduationproject.model.po.UserStatsPo;
import com.syt.graduationproject.model.po.VideoPo;
import com.syt.graduationproject.repository.UserRepository;
import com.syt.graduationproject.repository.VideoRepository;
import com.syt.graduationproject.util.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

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

    private final UserRepository userRepository;

    private final TransactionTemplate transactionTemplate;

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
                Boolean updated = transactionTemplate.execute(status -> {
                    int videoUpdated = videoRepository.incrVideoPlayCount(videoId, delta);
                    if (videoUpdated <= 0) {
                        videoRepository.insertVideoStatsIfAbsent(videoId);
                        videoUpdated = videoRepository.incrVideoPlayCount(videoId, delta);
                    }
                    if (videoUpdated <= 0) {
                        return false;
                    }

                    VideoPo videoPo = videoRepository.queryVideoById(videoId);
                    if (videoPo == null || videoPo.getUserId() == null) {
                        return true;
                    }

                    UserStatsPo userStatsPo = userRepository.queryUserStatsById(videoPo.getUserId());
                    if (userStatsPo == null) {
                        userRepository.initUserStats(videoPo.getUserId());
                    }
                    userRepository.updateUserPlayNum(videoPo.getUserId(), delta);
                    return true;
                });

                if (Boolean.TRUE.equals(updated)) {
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


