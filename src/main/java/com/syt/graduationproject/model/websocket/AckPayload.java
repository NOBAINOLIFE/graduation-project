package com.syt.graduationproject.model.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AckPayload {

    private String clientMsgId;

    private Long serverMsgId;

    private Long toUserId;

    private Boolean delivered;
}