package com.syt.graduationproject.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerAuditVideoRequest {

    private Long videoId;

    /**
     * 1-通过 0-驳回
     */
    private Integer operation;

    private String reviewNote;
}

