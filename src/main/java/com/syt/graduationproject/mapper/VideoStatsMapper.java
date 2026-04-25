package com.syt.graduationproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syt.graduationproject.model.po.VideoStatsPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface VideoStatsMapper extends BaseMapper<VideoStatsPo> {

    /**
     * 原子增加视频的评论数
     */
    @Update("UPDATE tb_video_stats SET comment_count = comment_count + 1 WHERE video_id = #{videoId}")
    int incrCommentCount(@Param("videoId") Long videoId);

    /**
     * 原子更新视频的点赞数
     */
    @Update("UPDATE tb_video_stats SET like_count = like_count + #{addNum} WHERE video_id = #{videoId}")
    int updateLikeCount(@Param("videoId") Long videoId, @Param("addNum") int addNum);

    @Update("UPDATE tb_video_stats SET coin_count = coin_count + #{addNum} WHERE video_id = #{videoId}")
    int updateCoinCount(@Param("videoId") Long videoId, @Param("addNum") int addNum);

    @Update("UPDATE tb_video_stats SET collect_count = GREATEST(collect_count + #{addNum}, 0) WHERE video_id = #{videoId}")
    int updateCollectCount(@Param("videoId") Long videoId, @Param("addNum") int addNum);

    @Update("UPDATE tb_video_stats SET comment_count = GREATEST(comment_count + #{addNum}, 0) WHERE video_id = #{videoId}")
    int updateCommentCount(@Param("videoId") Long videoId, @Param("addNum") int addNum);

    @Update("UPDATE tb_video_stats SET play_count = GREATEST(play_count + #{delta}, 0) WHERE video_id = #{videoId} AND is_deleted = 0")
    int updatePlayCount(@Param("videoId") Long videoId, @Param("delta") Long delta);

    int batchAddVideoCollectCount(@Param("videoIdList") List<Long> videoIdList,
                                  @Param("delta") Long delta);
}
