package com.syt.graduationproject.repository;


import com.syt.graduationproject.model.po.UserPo;

public interface UserRepository {

    /**
     * 根据账号查询用户
     */
    UserPo queryUserByAccount(Long account);
}
