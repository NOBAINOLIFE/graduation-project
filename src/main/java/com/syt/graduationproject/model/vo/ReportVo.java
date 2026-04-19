package com.syt.graduationproject.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportVo {

    private Long reportId;

    private Long reporterId;

    private Integer targetType;

    private Long targetId;

    private String reason;

    private String detail;

    private Integer status;

    private Long reviewerId;

    private String reviewNote;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

