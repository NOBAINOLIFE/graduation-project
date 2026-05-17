package com.syt.graduationproject.constant;

public interface RedisKeyTTLConstant {

    long PV_DEDUP_WINDOW_SECONDS = 30L;

    long PV_DELTA_KEY_TTL_HOURS = 24L;

    long PARTITION_LIST_CACHE_TTL_HOURS = 1L;

    long VIDEO_INFO_CACHE_TTL_MINUTES = 30L;

    long USER_INFO_CACHE_TTL_MINUTES = 30L;
}
