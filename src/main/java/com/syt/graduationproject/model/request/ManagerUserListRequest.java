package com.syt.graduationproject.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerUserListRequest {

    private String keyword;

    /**
     * 用户状态；不传或小于 0 时查询全部。
     */
    private Integer status;

    private Integer pageNum;

    private Integer pageSize;
}
