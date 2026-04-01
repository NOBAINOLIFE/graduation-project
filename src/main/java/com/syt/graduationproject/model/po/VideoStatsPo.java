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
 * 视频数据统计表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_video_stats")
public class VideoStatsPo {

    /**
     * 自增ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 视频ID
     */
    @TableField(value = "video_id")
    private Long videoId;

    /**
     * 播放量
     */
    @TableField(value = "play_count")
    private Long playCount;

    /**
     * 点赞数
     */
    @TableField(value = "like_count")
    private Long likeCount;

    /**
     * 投币数
     */
    @TableField(value = "coin_count")
    private Long coinCount;

    /**
     * 收藏数
     */
    @TableField(value = "collect_count")
    private Long collectCount;

    /**
     * 分享数
     */
    @TableField(value = "share_count")
    private Long shareCount;

    /**
     * 评论数
     */
    @TableField(value = "comment_count")
    private Long commentCount;

    /**
     * 状态 0-未删除 1-已删除
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

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