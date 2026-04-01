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
 * 视频信息表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_video")
public class VideoPo {
    /**
     * 唯一 id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发布者 ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 视频标题
     */
    @TableField(value = "title")
    private String title;

    /**
     * 视频描述
     */
    @TableField(value = "description")
    private String description;

    /**
     * 封面地址
     */
    @TableField(value = "cover_url")
    private String coverUrl;

    /**
     * 视频地址
     */
    @TableField(value = "video_url")
    private String videoUrl;

    /**
     * 视频时长 (秒)
     */
    @TableField(value = "duration")
    private Integer duration;

    /**
     * 播放数量
     */
    @TableField(value = "play_num")
    private Long playNum;

    /**
     * 点赞数量
     */
    @TableField(value = "like_num")
    private Long likeNum;

    /**
     * 评论数量
     */
    @TableField(value = "comment_num")
    private Long commentNum;

    /**
     * 收藏数量
     */
    @TableField(value = "collect_num")
    private Long collectNum;

    /**
     * 分享数量
     */
    @TableField(value = "share_num")
    private Long shareNum;

    /**
     * 状态 0-审核中 1-已发布 2-下架
     */
    @TableField(value = "status")
    private Integer status;

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