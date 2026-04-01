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
 * 点赞记录表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_like_record")
public class LikeRecordPo {
    /**
     * 唯一 id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 点赞用户 ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 目标类型 1-视频 2-评论
     */
    @TableField(value = "target_type")
    private Integer targetType;

    /**
     * 目标 ID
     */
    @TableField(value = "target_id")
    private Long targetId;

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