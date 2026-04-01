package com.syt.graduationproject.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.syt.graduationproject.mapper.FollowRecordMapper;
import com.syt.graduationproject.mapper.LikeRecordMapper;
import com.syt.graduationproject.mapper.UserStatsMapper;
import com.syt.graduationproject.model.po.FollowRecordPo;
import com.syt.graduationproject.model.po.UserStatsPo;
import com.syt.graduationproject.repository.InteractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import static com.syt.graduationproject.constant.CommonConstant.NOT_DELETED;

@Repository
@RequiredArgsConstructor
public class InteractRepositoryImpl implements InteractRepository {

    private final FollowRecordMapper followRecordMapper;

    private final LikeRecordMapper likeRecordMapper;

    private final UserStatsMapper userStatsMapper;

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
}
