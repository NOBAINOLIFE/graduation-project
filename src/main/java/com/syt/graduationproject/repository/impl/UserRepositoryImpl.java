package com.syt.graduationproject.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.syt.graduationproject.mapper.UserCoinChangeLogMapper;
import com.syt.graduationproject.mapper.UserMapper;
import com.syt.graduationproject.mapper.UserStatsMapper;
import com.syt.graduationproject.model.po.UserCoinChangeLogPo;
import com.syt.graduationproject.model.po.UserPo;
import com.syt.graduationproject.model.po.UserStatsPo;
import com.syt.graduationproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static com.syt.graduationproject.constant.CommonConstant.NOT_DELETED;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;

    private final UserStatsMapper userStatsMapper;

    private final UserCoinChangeLogMapper userCoinChangeLogMapper;

    /**
     * 根据账号查询用户
     */
    @Override
    public UserPo queryUserByAccount(Long account) {
        QueryWrapper<UserPo> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(UserPo::getAccount, account);
        return userMapper.selectOne(queryWrapper);
    }

    /**
     * 根据ID查询用户
     */
    @Override
    public UserPo queryUserById(Long userId) {
        QueryWrapper<UserPo> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(UserPo::getId, userId);
        return userMapper.selectOne(queryWrapper);
    }

    @Override
    public UserPo queryUserAnyStatusById(Long userId) {
        QueryWrapper<UserPo> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserPo::getId, userId);
        return userMapper.selectOne(queryWrapper);
    }

    /**
     * 根据ID查询用户统计信息
     */
    @Override
    public UserStatsPo queryUserStatsById(Long userId) {
        LambdaQueryWrapper<UserStatsPo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserStatsPo::getUserId, userId)
                .eq(UserStatsPo::getIsDeleted, NOT_DELETED);
        return userStatsMapper.selectOne(queryWrapper);
    }

    @Override
    public void updateUserPlayNum(Long userId, Long addNum) {
        userStatsMapper.updateUserPlayNum(userId, addNum);
    }

    @Override
    public void initUserStats(Long userId) {
        UserStatsPo userStatsPo = UserStatsPo.builder()
                .userId(userId)
                .build();
        userStatsMapper.insert(userStatsPo);
    }

    @Override
    public List<UserCoinChangeLogPo> queryUserCoinRecord(Long userId, LocalDateTime startTime) {
        return userCoinChangeLogMapper.selectList(new LambdaQueryWrapper<UserCoinChangeLogPo>()
                .eq(UserCoinChangeLogPo::getUserId, userId)
                .ge(UserCoinChangeLogPo::getCreateTime, startTime)
                .orderByDesc(UserCoinChangeLogPo::getCreateTime)
                .orderByDesc(UserCoinChangeLogPo::getId));
    }
}
