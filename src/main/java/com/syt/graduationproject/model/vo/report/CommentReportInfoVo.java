package com.syt.graduationproject.model.vo.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CommentReportInfoVo extends ReportInfoVo {

    /**
     * 被举报评论id
     */
    private Long commentId;

    /**
     * 被举报评论内容
     */
    private String content;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 是否是根评论
     */
    private Boolean isRootComment;

    /**
     * 根评论内容
     */
    private String rootCommentContent;

    /**
     * 关联视频id
     */
    private String videoId;

    /**
     * 视频封面地址
     */
    private String coverUrl;

    /**
     * 视频描述
     */
    private String description;

    /**
     * 视频标题
     */
    private String title;
}
