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
 * 评论信息表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_comment")
public class CommentPo {
    /**
     * 唯一 id
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 评论用户 ID
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
    @TableField(value = "target")
    private Long targetId;
    
    /**
     * 父评论 ID
     */
    @TableField(value = "parent_id")
    private Long parentId;
    
    /**
     * 评论内容
     */
    @TableField(value = "content")
    private String content;
    
    /**
     * 点赞数量
     */
    @TableField(value = "like_num")
    private Long likeNum;
    
    /**
     * 回复数量
     */
    @TableField(value = "reply_num")
    private Long replyNum;
    
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
