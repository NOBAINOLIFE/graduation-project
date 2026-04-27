package com.syt.graduationproject.model.vo.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerReportRecordVo {

    /**
     * 举报记录 id
     */
    private Long reportId;

    /**
     * 举报人 id
     */
    private Long reporterId;

    /**
     * 举报人名称
     */
    private String reporterName;

    /**
     * 举报人头像
     */
    private String reporterAvatar;

    /**
     * 举报人简介
     */
    private String reporterBio;

    /**
     * 举报类型
     */
    private Integer reportType;

    /**
     * 举报信息
     */
    private ReportInfoVo reportInfo;

    /**
     * 举报原因
     */
    private String reason;

    /**
     * 举报详情
     */
    private String detail;

    /**
     * 处理状态
     */
    private Integer status;

    /**
     * 处理人 id
     */
    private Long reviewerId;

    /**
     * 处理人名称
     */
    private String reviewerName;

    /**
     * 处理人头像
     */
    private String reviewerAvatar;

    /**
     * 审核备注
     */
    private String reviewNote;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
