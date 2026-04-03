package com.syt.graduationproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syt.graduationproject.model.po.CommentStatsPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CommentStatsMapper extends BaseMapper<CommentStatsPo> {

    /**
     * 原子增加根评论的回复数
     */
    @Update("UPDATE tb_comment_stats SET reply_count = reply_count + 1 WHERE comment_id = #{rootId}")
    int incrReplyCount(@Param("rootId") Long rootId);

    /**
     * 原子增加点赞数（预留）
     */
    @Update("UPDATE tb_comment_stats SET like_count = like_count + 1 WHERE comment_id = #{commentId}")
    int incrLikeCount(@Param("commentId") Long commentId);
}
