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
 * 用户观看历史与进度表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_user_video_history")
public class UserVideoHistoryPo {

    /**
     * 唯一 id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 视频ID
     */
    @TableField(value = "video_id")
    private Long videoId;

    /**
     * 最后播放位置（秒）
     */
    @TableField(value = "last_play_time")
    private Integer lastPlayTime;

    /**
     * 视频总时长（秒）
     */
    @TableField(value = "duration")
    private Integer duration;

    /**
     * 是否看完
     */
    @TableField(value = "is_finished")
    private Integer isFinished;

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