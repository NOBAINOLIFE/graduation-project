package com.syt.graduationproject.service;

import com.syt.graduationproject.model.bo.FollowBo;

public interface InteractService {

    /**
     * 查询关注关系
     */
    FollowBo queryFollow(Long followerId, Long followeeId);
}
