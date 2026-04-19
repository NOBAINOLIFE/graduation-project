package com.syt.graduationproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syt.graduationproject.model.po.UserWalletPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserWalletMapper extends BaseMapper<UserWalletPo> {

    @Update("UPDATE tb_user_wallet SET coin_balance = coin_balance + #{delta} WHERE user_id = #{userId}")
    int updateCoinBalance(@Param("userId") Long userId, @Param("delta") Long delta);

    @Update("UPDATE tb_user_wallet SET coin_balance = coin_balance - #{cost} WHERE user_id = #{userId} AND coin_balance >= #{cost}")
    int deductCoinIfEnough(@Param("userId") Long userId, @Param("cost") Long cost);
}

