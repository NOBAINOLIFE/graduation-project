package com.syt.graduationproject.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerReportListRequest {

    /**
     * 举报状态，默认查询待审核
     */
    private Integer status;

    /**
     * 举报目标类型，1-user 2-video
     */
    private Integer targetType;

    private Integer pageNum;

    private Integer pageSize;
}

