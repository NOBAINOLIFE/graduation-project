package com.syt.graduationproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syt.graduationproject.model.po.UserCoinChangeLogPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface UserCoinChangeLogMapper extends BaseMapper<UserCoinChangeLogPo> {

    @Select("SELECT COALESCE(SUM(-change_amount), 0) " +
            "FROM tb_user_coin_change_log " +
            "WHERE user_id = #{userId} " +
            "AND change_type = #{changeType} " +
            "AND video_id = #{videoId} " +
            "AND change_amount < 0")
    Integer sumConsumedCoinByTarget(@Param("userId") Long userId,
                                    @Param("changeType") Integer changeType,
                                    @Param("videoId") Long videoId);

    @Select("SELECT COUNT(1) " +
            "FROM tb_user_coin_change_log " +
            "WHERE user_id = #{userId} " +
            "AND change_type = #{changeType} " +
            "AND create_time >= #{startTime} " +
            "AND create_time < #{endTime}")
    Integer countLogsInRange(@Param("userId") Long userId,
                             @Param("changeType") Integer changeType,
                             @Param("startTime") LocalDateTime startTime,
                             @Param("endTime") LocalDateTime endTime);
}
