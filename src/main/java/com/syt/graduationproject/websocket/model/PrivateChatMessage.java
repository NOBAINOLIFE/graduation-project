package com.syt.graduationproject.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 服务端 -> 客户端：推送私聊消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivateChatMessage {

    /**
     * 服务端消息ID（落库主键）
     */
    private Long serverMsgId;

    private String clientMsgId;

    private Long fromUserId;

    private Long toUserId;

    private String content;

    private LocalDateTime sendTime;
}

