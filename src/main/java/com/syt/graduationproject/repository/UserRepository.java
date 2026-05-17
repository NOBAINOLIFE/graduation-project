package com.syt.graduationproject.repository;


import com.syt.graduationproject.model.po.UserCoinChangeLogPo;
import com.syt.graduationproject.model.po.UserPo;
import com.syt.graduationproject.model.po.UserStatsPo;

import java.time.LocalDateTime;
import java.util.List;

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

    /**
     * 更新用户总播放数
     */
    void updateUserPlayNum(Long userId, Long addNum);

    /**
     * 初始化用户统计信息
     */
    void initUserStats(Long userId);

    /**
     * 查询用户硬币流水
     */
    List<UserCoinChangeLogPo> queryUserCoinRecord(Long userId, LocalDateTime startTime);
}
