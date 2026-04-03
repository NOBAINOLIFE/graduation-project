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
 * 视频点赞关联表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_like_video")
public class LikeVideoPo {

    /**
     * 自增主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 点赞人ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 点赞视频ID
     */
    @TableField(value = "video_id")
    private Long videoId;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private LocalDateTime createTime;
}