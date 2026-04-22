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
 * 视频清晰度资源表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_video_source")
public class VideoSourcePo {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联视频主表ID
     */
    @TableField(value = "video_id")
    private Long videoId;

    /**
     * 清晰度标签: 360P, 480P, 720P, 1080P, 原画
     */
    @TableField(value = "resolution")
    private Integer resolution;

    /**
     * MinIO中的相对路径
     */
    @TableField(value = "play_url")
    private String playUrl;

    /**
     * 视频格式: mp4, m3u8, flv
     */
    @TableField(value = "format")
    private String format;

    /**
     * 编码格式: h264, h265
     */
    @TableField(value = "codec")
    private String codec;

    /**
     * 文件大小(Byte)
     */
    @TableField(value = "file_size")
    private Long fileSize;

    /**
     * 视频宽度
     */
    @TableField(value = "width")
    private Integer width;

    /**
     * 视频高度
     */
    @TableField(value = "height")
    private Integer height;

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