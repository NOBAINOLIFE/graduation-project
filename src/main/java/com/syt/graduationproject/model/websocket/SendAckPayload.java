package com.syt.graduationproject.model.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendAckPayload {

    private String clientMsgId;

    private Long serverMsgId;

    private Long toUserId;

    private Boolean delivered;

    private Integer status;

    private Integer failReason;

    private String failReasonText;
}
