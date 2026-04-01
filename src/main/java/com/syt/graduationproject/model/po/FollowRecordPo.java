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
 * 关注记录表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_follow_record")
public class FollowRecordPo {
    /**
     * 唯一 id
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 关注者 ID
     */
    @TableField(value = "follower_id")
    private Long followerId;
    
    /**
     * 被关注者 ID
     */
    @TableField(value = "followed_id")
    private Long followedId;
    
    /**
     * 状态 0-取消关注 1-有效
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
