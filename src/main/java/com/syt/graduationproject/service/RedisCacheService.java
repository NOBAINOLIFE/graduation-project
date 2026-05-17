package com.syt.graduationproject.service;

import com.syt.graduationproject.model.po.UserPo;
import com.syt.graduationproject.model.po.VideoPo;

public interface RedisCacheService {

    UserPo queryUserByIdWithCache(Long userId);

    VideoPo queryVideoByIdWithCache(Long videoId);
}
