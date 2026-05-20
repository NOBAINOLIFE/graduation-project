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
     * 被点赞内容 ID
     */
    @TableField(value = "target_id")
    private Long targetId;

    /**
     * 被点赞内容类型
     */
    @TableField(value = "target_type")
    private Integer targetType;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private LocalDateTime createTime;
}