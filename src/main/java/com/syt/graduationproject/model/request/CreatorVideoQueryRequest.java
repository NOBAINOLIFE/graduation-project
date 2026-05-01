package com.syt.graduationproject.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatorVideoQueryRequest {

    private String keyword;

    private Integer status;

    private Integer pageNum;

    private Integer pageSize;
}
