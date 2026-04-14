package com.syt.graduationproject.model.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadPayload {

    private Long readByUserId;

    private Long upToMsgId;
}