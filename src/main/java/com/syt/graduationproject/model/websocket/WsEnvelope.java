package com.syt.graduationproject.model.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 通用消息包装（便于前端区分消息类型）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WsEnvelope<T> {

    /**
     * 消息类型：chat / ack / error
     */
    private String type;

    /**
     * 实际载荷
     */
    private T data;
}

