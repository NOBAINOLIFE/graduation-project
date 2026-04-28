package com.syt.graduationproject.model.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论数据统计表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_comment_stats")
public class CommentStatsPo {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 视频ID
     */
    @TableField(value = "video_id")
    private Long videoId;
    /**
     * 评论ID
     */
    @TableField(value = "comment_id")
    private Long commentId;

    /**
     * 点赞数
     */
    @TableField(value = "like_count")
    private Long likeCount;

    /**
     * 根评论下的总回复数，非跟评论不维护
     */
    @TableField(value = "reply_count")
    private Long replyCount;

    /**
     * 评论热度分 = like_count * 1 + reply_count * 3 + 10
     */
    @TableField(value = "hot_score")
    private Long hotScore;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private LocalDateTime updateTime;
}
