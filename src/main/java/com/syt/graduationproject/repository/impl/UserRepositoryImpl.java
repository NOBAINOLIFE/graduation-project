package com.syt.graduationproject.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.syt.graduationproject.enums.UserStatusEnum;
import com.syt.graduationproject.mapper.UserMapper;
import com.syt.graduationproject.model.po.UserPo;
import com.syt.graduationproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import static com.syt.graduationproject.constant.CommonConstant.STATUS;
import static com.syt.graduationproject.constant.UserConstant.ACCOUNT;
import static com.syt.graduationproject.constant.UserConstant.USER_ID;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;

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

    @Override
    public UserPo queryUserById(Long userId) {
        QueryWrapper<UserPo> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(UserPo::getId, userId)
                .eq(UserPo::getStatus, UserStatusEnum.NORMAL.getCode());
        return userMapper.selectOne(queryWrapper);
    }
}
