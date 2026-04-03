package com.syt.graduationproject.service;

import com.syt.graduationproject.model.bo.FollowBo;
import com.syt.graduationproject.model.request.CommentRequest;
import com.syt.graduationproject.model.request.FollowRequest;
import org.springframework.transaction.annotation.Transactional;

public interface InteractService {

    /**
     * 查询关注关系
     */
    FollowBo queryFollow(Long followerId, Long followeeId);

    /**
     * 关注/取关
     */
    @Transactional(rollbackFor = Exception.class)
    void follow(FollowRequest request);

    /**
     * 评论/回复评论
     */
    @Transactional(rollbackFor = Exception.class)
    void comment(CommentRequest request);
}
