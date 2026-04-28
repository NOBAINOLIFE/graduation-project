package com.syt.graduationproject.model.vo.report;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserReportInfoVo extends ReportInfoVo {

    /**
     * 被举报用户视频数量
     */
    private Long videoNum;

    /**
     * 被举报用户粉丝数量
     */
    private Long fansNum;

    /**
     * 被举报用户点赞数量
     */
    private Long likeNum;

    /**
     * 被举报用户播放数量
     */
    private Long playNum;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
