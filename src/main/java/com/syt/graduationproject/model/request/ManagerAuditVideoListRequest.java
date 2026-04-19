package com.syt.graduationproject.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerAuditVideoListRequest {

    /**
     * 视频状态，默认查询审核中
     */
    private Integer status;

    private Integer pageNum;

    private Integer pageSize;
}

