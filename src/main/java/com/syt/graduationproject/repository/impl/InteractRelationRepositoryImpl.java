package com.syt.graduationproject.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.syt.graduationproject.mapper.CollectionItemMapper;
import com.syt.graduationproject.mapper.FollowRecordMapper;
import com.syt.graduationproject.mapper.LikeCommentMapper;
import com.syt.graduationproject.mapper.LikeVideoMapper;
import com.syt.graduationproject.mapper.UserCoinChangeLogMapper;
import com.syt.graduationproject.mapper.VideoStatsMapper;
import com.syt.graduationproject.model.po.CollectionItemPo;
import com.syt.graduationproject.model.po.FollowRecordPo;
import com.syt.graduationproject.model.po.LikeCommentPo;
import com.syt.graduationproject.model.po.LikeVideoPo;
import com.syt.graduationproject.model.po.VideoStatsPo;
import com.syt.graduationproject.repository.InteractRelationRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

import static com.syt.graduationproject.constant.CommonConstant.NOT_DELETED;

/**
 * 互动关系查询仓库实现
 */
@Repository
@RequiredArgsConstructor
public class InteractRelationRepositoryImpl implements InteractRelationRepository {

    private final LikeVideoMapper likeVideoMapper;

    private final CollectionItemMapper collectionItemMapper;

    private final FollowRecordMapper followRecordMapper;

    private final UserCoinChangeLogMapper userCoinChangeLogMapper;

    private final LikeCommentMapper likeCommentMapper;

    /**
     * 查询用户对某视频的点赞记录
     */
    @Override
    public LikeVideoPo queryLikeVideo(Long userId, Long videoId) {
        LambdaQueryWrapper<LikeVideoPo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LikeVideoPo::getUserId, userId)
                .eq(LikeVideoPo::getVideoId, videoId)
                .last("LIMIT 1");
        return likeVideoMapper.selectOne(queryWrapper);
    }

    /**
     * 查询用户对某视频的收藏记录
     */
    @Override
    public List<CollectionItemPo> queryCollectVideo(Long userId, Long videoId) {
        LambdaQueryWrapper<CollectionItemPo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CollectionItemPo::getUserId, userId)
                .eq(CollectionItemPo::getVideoId, videoId)
                .eq(CollectionItemPo::getIsDeleted, NOT_DELETED);
        return collectionItemMapper.selectList(queryWrapper);
    }

    /**
     * 查询两个用户之间的关注关系
     */
    @Override
    public FollowRecordPo queryFollowRecord(Long followerId, Long followeeId) {
        LambdaQueryWrapper<FollowRecordPo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FollowRecordPo::getFollowerId, followerId)
                .eq(FollowRecordPo::getFolloweeId, followeeId)
                .eq(FollowRecordPo::getIsDeleted, NOT_DELETED)
                .last("LIMIT 1");
        return followRecordMapper.selectOne(queryWrapper);
    }

    /**
     * 查询用户对视频的投币数量
     */
    @Override
    public Integer queryUserCoinCountForVideo(Long userId, Long videoId) {
        // change_type: 2-视频投币
        Integer consumedCoins = userCoinChangeLogMapper.sumConsumedCoinByTarget(userId, 2, videoId);
        return consumedCoins != null ? consumedCoins : 0;
    }

    @Override
    public List<LikeCommentPo> batchQueryLikeComment(Long userId, List<Long> commentIdList) {
        if (userId == null || CollectionUtils.isEmpty(commentIdList)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<LikeCommentPo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LikeCommentPo::getUserId, userId)
                .in(LikeCommentPo::getCommentId, commentIdList);
        return likeCommentMapper.selectList(queryWrapper);
    }
}
