package com.syt.graduationproject.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisJwtWhitelistUtil {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String PREFIX = "jwt:whitelist:";

    public void addToken(String token, long expireMillis) {
        stringRedisTemplate.opsForValue().set(PREFIX + token, "1", expireMillis, TimeUnit.MILLISECONDS);
    }

    public boolean isTokenWhitelisted(String token) {
        return stringRedisTemplate.hasKey(PREFIX + token);
    }

    public void removeToken(String token) {
        stringRedisTemplate.delete(PREFIX + token);
    }
}

