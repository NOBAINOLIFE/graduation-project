package com.syt.graduationproject.model.vo.report;

import com.syt.graduationproject.model.vo.VideoSourceVo;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;


@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class VideoReportInfoVo extends ReportInfoVo {

    /**
     * 被举报视频id
     */
    private Long videoId;

    /**
     * 被举报视频标题
     */
    private String title;

    /**
     * 被举报视频封面地址
     */
    private String coverUrl;

    /**
     * 被举报视频描述
     */
    private String description;

    /**
     * 被举报视频分区名称
     */
    private String partitionName;

    /**
     * 被举报视频标签列表
     */
    private List<String> tagList;

    /**
     * 被举报视频播放源
     */
    private List<VideoSourceVo> sourceList;

    /**
     * 被举报视频播放次数
     */
    private Long playCount;

    /**
     * 被举报视频点赞次数
     */
    private Long likeCount;

    /**
     * 被举报视频收藏次数
     */
    private Long collectCount;

    /**
     * 被举报视频评论次数
     */
    private Long commentCount;

    /**
     * 被举报视频分享次数
     */
    private Long shareCount;

    /**
     * 被举报视频创建时间
     */
    private String createTime;
}
