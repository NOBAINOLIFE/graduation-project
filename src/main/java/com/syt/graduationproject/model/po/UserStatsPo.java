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
 * 用户数据统计表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_user_stats")
public class UserStatsPo {

    /**
     * 自增ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 硬币数量
     */
    @TableField(value = "coin_num")
    private Long coinNum;

    /**
     * 视频数量
     */
    @TableField(value = "video_num")
    private Long videoNum;

    /**
     * 点赞数量
     */
    @TableField(value = "like_num")
    private Long likeNum;

    /**
     * 关注人数
     */
    @TableField(value = "follow_num")
    private Long followNum;

    /**
     * 粉丝人数
     */
    @TableField(value = "fans_num")
    private Long fansNum;

    /**
     * 总播放数
     */
    @TableField(value = "play_num")
    private Long playNum;

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