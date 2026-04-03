package com.syt.graduationproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syt.graduationproject.model.po.VideoStatsPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface VideoStatsMapper extends BaseMapper<VideoStatsPo> {

    /**
     * 原子增加视频的评论数
     */
    @Update("UPDATE tb_video_stats SET comment_count = comment_count + 1 WHERE video_id = #{videoId}")
    int incrCommentCount(@Param("videoId") Long videoId);
}
