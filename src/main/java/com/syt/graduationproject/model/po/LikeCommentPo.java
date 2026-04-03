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
 * 评论点赞明细表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_like_comment")
public class LikeCommentPo {

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
     * 评论 ID
     */
    @TableField(value = "comment_id")
    private Long commentId;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private LocalDateTime createTime;
}