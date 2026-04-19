package com.syt.graduationproject.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.syt.graduationproject.enums.UserStatusEnum;
import com.syt.graduationproject.mapper.UserMapper;
import com.syt.graduationproject.mapper.UserStatsMapper;
import com.syt.graduationproject.model.po.UserPo;
import com.syt.graduationproject.model.po.UserStatsPo;
import com.syt.graduationproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import static com.syt.graduationproject.constant.CommonConstant.NOT_DELETED;
import static com.syt.graduationproject.constant.CommonConstant.STATUS;
import static com.syt.graduationproject.constant.UserConstant.ACCOUNT;
import static com.syt.graduationproject.constant.UserConstant.USER_ID;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;

    private final UserStatsMapper userStatsMapper;

    /**
     * 根据账号查询用户
     */
    @Override
    public UserPo queryUserByAccount(Long account) {
        QueryWrapper<UserPo> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(UserPo::getAccount, account)
                .eq(UserPo::getStatus, UserStatusEnum.NORMAL.getCode());
        return userMapper.selectOne(queryWrapper);
    }

    /**
     * 根据ID查询用户
     */
    @Override
    public UserPo queryUserById(Long userId) {
        QueryWrapper<UserPo> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(UserPo::getId, userId)
                .eq(UserPo::getStatus, UserStatusEnum.NORMAL.getCode());
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
}
