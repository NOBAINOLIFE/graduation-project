package com.syt.graduationproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syt.graduationproject.model.po.CommentStatsPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CommentStatsMapper extends BaseMapper<CommentStatsPo> {

    /**
     * 原子增加根评论的回复数
     */
    @Update("UPDATE tb_comment_stats SET reply_count = reply_count + 1, hot_score = like_count + (reply_count + 1) * 3 + 10 WHERE comment_id = #{rootId}")
    int incrReplyCount(@Param("rootId") Long rootId);

    /**
     * 原子更新评论的点赞数
     */
    @Update("UPDATE tb_comment_stats SET like_count = GREATEST(like_count + #{addNum}, 0), hot_score = GREATEST(like_count + #{addNum}, 0) + reply_count * 3 + 10 WHERE comment_id = #{commentId}")
    int updateLikeCount(@Param("commentId") Long commentId, @Param("addNum") int addNum);

    @Update("UPDATE tb_comment_stats SET reply_count = GREATEST(reply_count + #{addNum}, 0), hot_score = like_count + GREATEST(reply_count + #{addNum}, 0) * 3 + 10 WHERE comment_id = #{commentId}")
    int updateReplyCount(@Param("commentId") Long commentId, @Param("addNum") int addNum);

    List<CommentStatsPo> queryRootCommentStatsByHot(@Param("videoId") Long videoId,
                                                    @Param("lastHotScore") Long lastHotScore,
                                                    @Param("lastCreateTime") LocalDateTime lastCreateTime,
                                                    @Param("lastCommentId") Long lastCommentId,
                                                    @Param("pageSize") Integer pageSize);
}
