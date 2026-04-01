package com.syt.graduationproject.service;

import com.syt.graduationproject.model.bo.FollowBo;
import com.syt.graduationproject.model.request.FollowRequest;

public interface InteractService {

    /**
     * 查询关注关系
     */
    FollowBo queryFollow(Long followerId, Long followeeId);

    /**
     * 关注/取关
     */
    void follow(FollowRequest request);
}
