package com.syt.graduationproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syt.graduationproject.model.po.UserBlockPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserBlockMapper extends BaseMapper<UserBlockPo> {

	@Select("SELECT COUNT(1) FROM tb_user_block WHERE user_id = #{userId} AND blocked_user_id = #{targetUserId} AND is_deleted = 0")
	Long countActiveBlock(@Param("userId") Long userId, @Param("targetUserId") Long targetUserId);

	@Select("SELECT COUNT(1) FROM tb_user_block WHERE ((user_id = #{userA} AND blocked_user_id = #{userB}) OR (user_id = #{userB} AND blocked_user_id = #{userA})) AND is_deleted = 0")
	Long countAnyDirectionBlock(@Param("userA") Long userA, @Param("userB") Long userB);

	@Select({
			"<script>",
			"SELECT DISTINCT CASE WHEN user_id = #{userId} THEN blocked_user_id ELSE user_id END",
			"FROM tb_user_block",
			"WHERE is_deleted = 0",
			"AND (",
			"  (user_id = #{userId} AND blocked_user_id IN",
			"    <foreach collection='candidateUserIds' item='candidateUserId' open='(' separator=',' close=')'>#{candidateUserId}</foreach>",
			"  )",
			"  OR",
			"  (blocked_user_id = #{userId} AND user_id IN",
			"    <foreach collection='candidateUserIds' item='candidateUserId' open='(' separator=',' close=')'>#{candidateUserId}</foreach>",
			"  )",
			")",
			"</script>"
	})
	List<Long> listMutualBlockedUserIds(@Param("userId") Long userId, @Param("candidateUserIds") List<Long> candidateUserIds);
}


