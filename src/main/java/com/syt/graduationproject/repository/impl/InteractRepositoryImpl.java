package com.syt.graduationproject.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.syt.graduationproject.mapper.CollectionDirectoryMapper;
import com.syt.graduationproject.mapper.CollectionItemMapper;
import com.syt.graduationproject.mapper.FollowRecordMapper;
import com.syt.graduationproject.mapper.UserStatsMapper;
import com.syt.graduationproject.model.po.CollectionDirectoryPo;
import com.syt.graduationproject.model.po.CollectionItemPo;
import com.syt.graduationproject.model.po.FollowRecordPo;
import com.syt.graduationproject.model.po.UserStatsPo;
import com.syt.graduationproject.repository.InteractRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

import static com.syt.graduationproject.constant.CommonConstant.NOT_DELETED;

@Repository
@RequiredArgsConstructor
public class InteractRepositoryImpl implements InteractRepository {

    private final FollowRecordMapper followRecordMapper;

    private final UserStatsMapper userStatsMapper;

    private final CollectionItemMapper collectionItemMapper;

    private final CollectionDirectoryMapper collectionDirectoryMapper;

    /**
     * 查询两者关注关系
     */
    @Override
    public FollowRecordPo queryFollow(Long followerId, Long followeeId) {
        QueryWrapper<FollowRecordPo> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(FollowRecordPo::getFollowerId, followerId)
                .eq(FollowRecordPo::getFolloweeId, followeeId)
                .eq(FollowRecordPo::getIsDeleted, NOT_DELETED);
        return followRecordMapper.selectOne(queryWrapper);
    }

    /**
     * 查询用户粉丝数
     */
    @Override
    public Long queryUserFansNum(Long userId) {
        QueryWrapper<UserStatsPo> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(UserStatsPo::getUserId, userId)
                .eq(UserStatsPo::getIsDeleted, NOT_DELETED);
        return userStatsMapper.selectOne(queryWrapper).getFansNum();
    }

    /**
     * 查询用户关注数
     */
    @Override
    public Long queryUserFollowNum(Long userId) {
        QueryWrapper<UserStatsPo> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(UserStatsPo::getUserId, userId)
                .eq(UserStatsPo::getIsDeleted, NOT_DELETED);
        return userStatsMapper.selectOne(queryWrapper).getFollowNum();
    }

    /**
     * 查询用户点赞数
     */
    @Override
    public Long queryUserLikeNum(Long userId) {
        QueryWrapper<UserStatsPo> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(UserStatsPo::getUserId, userId)
                .eq(UserStatsPo::getIsDeleted, NOT_DELETED);
        return userStatsMapper.selectOne(queryWrapper).getLikeNum();
    }

    /**
     * 更新用户粉丝数
     */
    @Override
    public void updateUserFansNum(Long userId, Long addNum) {
        userStatsMapper.updateUserFansNum(userId, addNum);
    }

    /**
     * 更新用户点赞数
     */
    @Override
    public void updateUserLikeNum(Long userId, Long addNum) {
        userStatsMapper.updateUserLikeNum(userId, addNum);
    }

    /**
     * 更新用户关注数
     */
    @Override
    public void updateUserFollowNum(Long userId, Long addNum) {
        userStatsMapper.updateUserFollowNum(userId, addNum);
    }

    /**
     * 更新用户视频数
     */
    @Override
    public void updateUserVideoNum(Long userId, Long addNum) {
        userStatsMapper.updateUserVideoNum(userId, addNum);
    }

    /**
     * 初始化用户统计信息
     */
    @Override
    public void initUserStats(Long userId) {
        UserStatsPo userStatsPo = UserStatsPo.builder()
                .userId(userId)
                .build();
        userStatsMapper.insert(userStatsPo);
    }

    /**
     * 查询用户收藏记录（不管是否删除）
     */
    @Override
    public List<CollectionItemPo> queryUserCollectRecord(Long userId, Long videoId, Long directoryId) {
        LambdaQueryWrapper<CollectionItemPo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CollectionItemPo::getUserId, userId);
        if (videoId != null) {
            queryWrapper.eq(CollectionItemPo::getVideoId, videoId);
        }
        if (directoryId != null) {
            queryWrapper.eq(CollectionItemPo::getDirectoryId, directoryId);
        }
        return collectionItemMapper.selectList(queryWrapper);
    }

    @Override
    public List<CollectionDirectoryPo> queryUserCollectionDirectory(Long userId) {
        return collectionDirectoryMapper.selectList(new LambdaQueryWrapper<CollectionDirectoryPo>()
                .eq(CollectionDirectoryPo::getUserId, userId)
                .eq(CollectionDirectoryPo::getIsDeleted, NOT_DELETED));
    }

    @Override
    public List<CollectionItemPo> queryUserCollectionItemWithVideo(Long userId, Long videoId) {
        return collectionItemMapper.selectList(new LambdaQueryWrapper<CollectionItemPo>()
                .eq(CollectionItemPo::getUserId, userId)
                .eq(CollectionItemPo::getVideoId, videoId)
                .eq(CollectionItemPo::getIsDeleted, NOT_DELETED));
    }

    /**
     * 判断用户是否收藏某视频
     */
    @Override
    public boolean isCollectVideo(Long userId, Long videoId) {
        return collectionItemMapper.selectOne(new LambdaQueryWrapper<CollectionItemPo>()
                .eq(CollectionItemPo::getUserId, userId)
                .eq(CollectionItemPo::getVideoId, videoId)
                .eq(CollectionItemPo::getIsDeleted, NOT_DELETED)
                .last("LIMIT 1")) != null;
    }

    @Override
    public List<CollectionDirectoryPo> batchQueryUserCollectionDirectory(Long userId, List<Long> directoryIdList) {
        return collectionDirectoryMapper.selectList(new LambdaQueryWrapper<CollectionDirectoryPo>()
                .eq(CollectionDirectoryPo::getUserId, userId)
                .in(CollectionUtils.isNotEmpty(directoryIdList), CollectionDirectoryPo::getId, directoryIdList)
                .eq(CollectionDirectoryPo::getIsDeleted, NOT_DELETED));
    }

    /**
     * 批量往一个收藏夹内添加视频
     */
    @Override
    public int batchCollectVideo(Long userId, Long directoryId, List<Long> videoIdList) {
        return collectionItemMapper.batchCollectVideo(userId, directoryId, videoIdList);
    }

    /**
     * 批量取消收藏一个收藏夹内的视频
     */
    @Override
    public int batchCancelCollectVideo(Long userId, Long directoryId, List<Long> videoIdList) {
        return collectionItemMapper.batchCancelCollectVideo(userId, directoryId, videoIdList);
    }
}
