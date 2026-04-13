package com.syt.graduationproject.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 客户端 -> 服务端：发送私聊消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivateChatSendRequest {

    /**
     * 前端生成的消息ID（用于幂等/回执关联，可选）
     */
    private String clientMsgId;

    /**
     * 接收方用户ID
     */
    private Long toUserId;

    /**
     * 文本内容
     */
    private String content;
}

