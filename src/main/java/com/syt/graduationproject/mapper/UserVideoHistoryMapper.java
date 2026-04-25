package com.syt.graduationproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syt.graduationproject.model.po.UserVideoHistoryPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserVideoHistoryMapper extends BaseMapper<UserVideoHistoryPo> {

	@Insert("INSERT INTO tb_user_video_history (user_id, video_id, last_play_time, duration, is_finished) " +
			"VALUES (#{userId}, #{videoId}, #{lastPlayTime}, #{duration}, #{isFinished}) " +
			"ON DUPLICATE KEY UPDATE " +
			"last_play_time = VALUES(last_play_time), " +
			"duration = VALUES(duration), " +
			"is_finished = VALUES(is_finished), " +
			"is_deleted = 0")
	int upsertHistory(@Param("userId") Long userId,
					  @Param("videoId") Long videoId,
					  @Param("lastPlayTime") Integer lastPlayTime,
					  @Param("duration") Integer duration,
					  @Param("isFinished") Integer isFinished);
}
