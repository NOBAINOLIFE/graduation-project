package com.syt.graduationproject.repository;


import com.syt.graduationproject.model.po.UserPo;
import com.syt.graduationproject.model.po.UserStatsPo;

public interface UserRepository {

    /**
     * 根据账号查询用户
     */
    UserPo queryUserByAccount(Long account);

    /**
     * 根据ID查询用户
     */
    UserPo queryUserById(Long userId);

    /**
     * 根据ID查询用户（不限制状态）
     */
    UserPo queryUserAnyStatusById(Long userId);

    /**
     * 根据ID查询用户统计信息
     */
    UserStatsPo queryUserStatsById(Long userId);
}
