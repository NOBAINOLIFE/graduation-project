package com.syt.graduationproject.repository;

import com.syt.graduationproject.model.po.CollectionItemPo;
import com.syt.graduationproject.model.po.FollowRecordPo;
import com.syt.graduationproject.model.po.LikeCommentPo;
import com.syt.graduationproject.model.po.LikeVideoPo;

import java.util.List;

/**
 * 互动关系查询仓库
 * 专门用于组装和查询互动关系相关的SQL
 */
public interface InteractRelationRepository {

    /**
     * 查询用户是否点赞了指定视频
     *
     * @param userId  用户ID
     * @param videoId 视频ID
     * @return 点赞记录，不存在则返回null
     */
    LikeVideoPo queryLikeVideo(Long userId, Long videoId);

    /**
     * 查询用户是否收藏了指定视频（未删除的记录）
     *
     * @param userId  用户ID
     * @param videoId 视频ID
     * @return 收藏记录列表
     */
    List<CollectionItemPo> queryCollectVideo(Long userId, Long videoId);

    /**
     * 查询两个用户之间的关注关系
     *
     * @param followerId 关注者ID
     * @param followeeId 被关注者ID
     * @return 关注记录，不存在则返回null
     */
    FollowRecordPo queryFollowRecord(Long followerId, Long followeeId);

    /**
     * 查询用户对视频的投币数量
     *
     * @param userId  用户ID
     * @param videoId 视频ID
     * @return 投币数量
     */
    Integer queryUserCoinCountForVideo(Long userId, Long videoId);

    /**
     * 查询用户对某些评论的点赞记录
     *
     * @param userId      用户ID
     * @param commentIdList 评论ID列表
     * @return 点赞记录列表
     */
    List<LikeCommentPo> batchQueryLikeComment(Long userId, List<Long> commentIdList);
}
