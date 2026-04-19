package com.syt.graduationproject.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSubmitRequest {

    /**
     * 举报目标类型：1-用户 2-视频 3-评论
     */
    private Integer targetType;

    /**
     * 举报目标ID
     */
    private Long targetId;

    /**
     * 举报原因
     */
    private String reason;

    /**
     * 举报补充信息
     */
    private String detail;
}

