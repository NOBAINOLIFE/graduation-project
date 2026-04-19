package com.syt.graduationproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syt.graduationproject.model.po.UserBlockPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserBlockMapper extends BaseMapper<UserBlockPo> {

	@Select("SELECT COUNT(1) FROM tb_user_block WHERE user_id = #{userId} AND blocked_user_id = #{targetUserId} AND is_deleted = 0")
	Long countActiveBlock(@Param("userId") Long userId, @Param("targetUserId") Long targetUserId);

	@Select("SELECT COUNT(1) FROM tb_user_block WHERE ((user_id = #{userA} AND blocked_user_id = #{userB}) OR (user_id = #{userB} AND blocked_user_id = #{userA})) AND is_deleted = 0")
	Long countAnyDirectionBlock(@Param("userA") Long userA, @Param("userB") Long userB);
}


