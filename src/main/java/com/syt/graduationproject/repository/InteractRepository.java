package com.syt.graduationproject.repository;

import com.syt.graduationproject.model.po.FollowRecordPo;

public interface InteractRepository {

    /**
     * 查询两者关注关系
     */
    FollowRecordPo queryFollow(Long followerId, Long followeeId);

    /**
     * 查询用户粉丝数量
     */
    Long queryUserFansNum(Long myId);

    /**
     * 查询用户关注数量
     */
    Long queryUserFollowNum(Long myId);

    /**
     * 查询用户获赞数量
     */
    Long queryUserLikeNum(Long myId);
}
