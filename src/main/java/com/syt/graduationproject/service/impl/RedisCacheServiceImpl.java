package com.syt.graduationproject.service.impl;

import com.syt.graduationproject.model.po.UserPo;
import com.syt.graduationproject.model.po.VideoPo;
import com.syt.graduationproject.repository.UserRepository;
import com.syt.graduationproject.repository.VideoRepository;
import com.syt.graduationproject.service.RedisCacheService;
import com.syt.graduationproject.util.JsonUtil;
import com.syt.graduationproject.util.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.syt.graduationproject.constant.RedisKeyTTLConstant.USER_INFO_CACHE_TTL_MINUTES;
import static com.syt.graduationproject.constant.RedisKeyTTLConstant.VIDEO_INFO_CACHE_TTL_MINUTES;

@Service
@RequiredArgsConstructor
public class RedisCacheServiceImpl implements RedisCacheService {

    private final RedisTemplate<String, String> stringRedisTemplate;

    private final UserRepository userRepository;

    private final VideoRepository videoRepository;

    @Override
    public UserPo queryUserByIdWithCache(Long userId) {
        String cacheKey = RedisKeyUtil.userInfoKey(userId);
        String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.isNotBlank(cachedJson)) {
            return JsonUtil.fromJson(cachedJson, UserPo.class);
        }
        UserPo userPo = userRepository.queryUserById(userId);
        if (userPo != null) {
            userPo.setPassword(null);
            stringRedisTemplate.opsForValue().set(cacheKey, JsonUtil.toJson(userPo),
                    USER_INFO_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        }
        return userPo;
    }

    @Override
    public VideoPo queryVideoByIdWithCache(Long videoId) {
        String cacheKey = RedisKeyUtil.videoInfoKey(videoId);
        String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.isNotBlank(cachedJson)) {
            return JsonUtil.fromJson(cachedJson, VideoPo.class);
        }
        VideoPo videoPo = videoRepository.queryVideoById(videoId);
        if (videoPo != null) {
            stringRedisTemplate.opsForValue().set(cacheKey, JsonUtil.toJson(videoPo),
                    VIDEO_INFO_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        }
        return videoPo;
    }
}
