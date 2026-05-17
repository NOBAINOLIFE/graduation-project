package com.syt.graduationproject.service;

import com.syt.graduationproject.model.bo.FollowBo;
import com.syt.graduationproject.model.bo.UserVideoInteractionBo;

import java.util.List;
import java.util.Set;

/**
 * 互动关系查询服务
 * 专门用于查询用户与视频、用户之间的各种互动关系状态
 */
public interface InteractRelationService {

    /**
     * 查询两个用户之间的关注关系
     */
    FollowBo queryFollowRelation(Long followerId, Long followeeId);

    /**
     * 查询用户是否点赞了指定视频
     *
     * @param userId  用户ID
     * @param videoId 视频ID
     * @return 是否点赞
     */
    boolean isLikeVideo(Long userId, Long videoId);

    /**
     * 查询用户是否收藏了指定视频
     *
     * @param userId  用户ID
     * @param videoId 视频ID
     * @return 是否收藏
     */
    boolean isCollectVideo(Long userId, Long videoId);

    /**
     * 查询用户对视频的投币数量
     *
     * @param userId  用户ID
     * @param videoId 视频ID
     * @return 投币数量
     */
    Integer getUserCoinCountForVideo(Long userId, Long videoId);

    /**
     * 查询用户是否点赞了指定评论
     *
     * @param userId        用户ID
     * @param commentIdList 评论ID列表
     * @return 点赞了的评论的ID集合
     */
    Set<Long> batchQueryLikeComment(Long userId, List<Long> commentIdList);

    /**
     * 查询用户与视频的完整交互信息
     *
     * @param userId  用户ID
     * @param videoId 视频ID
     * @return 用户视频交互信息
     */
    UserVideoInteractionBo queryUserVideoInteraction(Long userId, Long videoId);
}
