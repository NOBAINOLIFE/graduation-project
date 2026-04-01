package com.syt.graduationproject.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.syt.graduationproject.mapper.FollowRecordMapper;
import com.syt.graduationproject.mapper.LikeRecordMapper;
import com.syt.graduationproject.model.po.FollowRecordPo;
import com.syt.graduationproject.model.po.LikeRecordPo;
import com.syt.graduationproject.repository.InteractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import static com.syt.graduationproject.mapper.FollowRecordMapper.FOLLOW;
import static com.syt.graduationproject.mapper.LikeRecordMapper.LIKE;

@Repository
@RequiredArgsConstructor
public class InteractRepositoryImpl implements InteractRepository {

    private final FollowRecordMapper followRecordMapper;

    private final LikeRecordMapper likeRecordMapper;

    @Override
    public FollowRecordPo queryFollow(Long followerId, Long followeeId) {
        QueryWrapper<FollowRecordPo> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(FollowRecordPo::getFollowerId, followerId)
                .eq(FollowRecordPo::getFolloweeId, followeeId)
                .eq(FollowRecordPo::getStatus, FOLLOW);
        return followRecordMapper.selectOne(queryWrapper);
    }

    @Override
    public Long queryUserFansNum(Long userId) {
        QueryWrapper<FollowRecordPo> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(FollowRecordPo::getFolloweeId, userId)
                .eq(FollowRecordPo::getStatus, FOLLOW);
        return followRecordMapper.selectCount(queryWrapper);
    }

    @Override
    public Long queryUserFollowNum(Long userId) {
        QueryWrapper<FollowRecordPo> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(FollowRecordPo::getFollowerId, userId)
                .eq(FollowRecordPo::getStatus, FOLLOW);
        return followRecordMapper.selectCount(queryWrapper);
    }

    @Override
    public Long queryUserLikeNum(Long userId) {
        QueryWrapper<LikeRecordPo> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(LikeRecordPo::getUserId, userId)
                .eq(LikeRecordPo::getStatus, LIKE);
        return likeRecordMapper.selectCount(queryWrapper);
    }
}
