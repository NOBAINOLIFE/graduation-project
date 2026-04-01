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
import java.util.Date;

/**
 * 收藏记录表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_collect_record")
public class CollectRecordPo {
    /**
     * 唯一 id
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 收藏用户 ID
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
     * 状态 0-取消 1-有效
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
