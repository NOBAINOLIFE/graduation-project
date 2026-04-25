package com.syt.graduationproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syt.graduationproject.model.po.UserStatsPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserStatsMapper extends BaseMapper<UserStatsPo> {

    /**
     * 更新用户粉丝数
     */
    @Update("update tb_user_stats set fans_num = fans_num + #{addNum} where user_id = #{userId}")
    void updateUserFansNum(@Param("userId") Long userId, @Param("addNum") Long addNum);

    /**
     * 更新用户关注数
     */
    @Update("update tb_user_stats set follow_num = follow_num + #{addNum} where user_id = #{userId}")
    void updateUserFollowNum(@Param("userId") Long userId, @Param("addNum") Long addNum);

    /**
     * 更新用户获赞数
     */
    @Update("update tb_user_stats set like_num = like_num + #{addNum} where user_id = #{userId}")
    void updateUserLikeNum(@Param("userId") Long userId, @Param("addNum") Long addNum);

    /**
     * 更新用户视频数
     */
    @Update("update tb_user_stats set video_num = video_num + #{addNum} where user_id = #{userId}")
    void updateUserVideoNum(@Param("userId") Long userId, @Param("addNum") Long addNum);

    /**
     * 更新用户播放数
     */
    @Update("update tb_user_stats set play_num = play_num + #{addNum} where user_id = #{userId}")
    void updateUserPlayNum(@Param("userId") Long userId, @Param("addNum") Long addNum);
}
