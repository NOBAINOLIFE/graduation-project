package com.syt.graduationproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syt.graduationproject.model.po.UserBlackPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserBlackMapper extends BaseMapper<UserBlackPo> {

	Long countActiveBlock(@Param("userId") Long userId, @Param("targetUserId") Long targetUserId);

	Long countAnyDirectionBlock(@Param("userA") Long userA, @Param("userB") Long userB);

	List<Long> listMutualBlockedUserIds(@Param("userId") Long userId, @Param("candidateUserIds") List<Long> candidateUserIds);
}


