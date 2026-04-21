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
     * 视频时长 (秒)
     */
    @TableField(value = "duration")
    private Integer duration;

    /**
     * 视频分区ID
     */
    @TableField(value = "partition_id")
    private Long partitionId;

    /**
     * 状态 0-待上传 1-上传完成 2-转码中 3-审核中 4-已发布 5-封禁 6-删除
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