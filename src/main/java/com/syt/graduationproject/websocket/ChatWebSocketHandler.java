package com.syt.graduationproject.websocket;

import com.syt.graduationproject.service.InteractService;
import com.syt.graduationproject.util.JsonUtil;
import com.syt.graduationproject.websocket.model.PrivateChatSendRequest;
import com.syt.graduationproject.websocket.model.WsEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

import static com.syt.graduationproject.websocket.ChatWsConstants.ATTR_USER_ID;

/**
 * 私聊 WebSocket 处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final InteractService interactService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getUserId(session);
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("未认证"));
            return;
        }
        interactService.registerChatSession(userId, session);
        log.info("WebSocket 连接建立，userId: {}, sessionId: {}", userId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long fromUserId = getUserId(session);
        if (fromUserId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("未认证"));
            return;
        }
        String payload = message.getPayload();
        if (StringUtils.isBlank(payload)) {
            return;
        }

        try {
            // 兼容两种格式：
            // 1) 旧格式：{"clientMsgId":"...","toUserId":2,"content":"hi"}
            // 2) 新格式：{"type":"chat|ack|ping","data":{...}}
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = (Map<String, Object>) JsonUtil.fromJson(payload, Map.class);
            Object typeObj = raw.get("type");
            if (typeObj == null) {
                PrivateChatSendRequest req = JsonUtil.fromJson(payload, PrivateChatSendRequest.class);
                interactService.sendPrivateChat(fromUserId, req);
                return;
            }

            String type = String.valueOf(typeObj);
            Object data = raw.get("data");
            if ("chat".equalsIgnoreCase(type)) {
                String dataJson = JsonUtil.toJson(data);
                PrivateChatSendRequest req = JsonUtil.fromJson(dataJson, PrivateChatSendRequest.class);
                interactService.sendPrivateChat(fromUserId, req);
                return;
            }
            if ("ack".equalsIgnoreCase(type)) {
                if (data instanceof Map) {
                    Map<?, ?> dataMap = (Map<?, ?>) data;
                    Object serverMsgId = dataMap.get("serverMsgId");
                    if (serverMsgId != null) {
                        interactService.ackPrivateMessage(fromUserId, Long.valueOf(String.valueOf(serverMsgId)));
                    }
                }
                return;
            }
            if ("ping".equalsIgnoreCase(type)) {
                interactService.heartbeat(fromUserId, session);
                WsEnvelope<Object> pong = WsEnvelope.builder()
                        .type("pong")
                        .data(System.currentTimeMillis())
                        .build();
                session.sendMessage(new TextMessage(JsonUtil.toJson(pong)));
                return;
            }
            if ("read".equalsIgnoreCase(type)) {
                // {"type":"read","data":{"withUserId":2,"upToMsgId":123}}
                if (data instanceof Map) {
                    Map<?, ?> dataMap = (Map<?, ?>) data;
                    Object withUserId = dataMap.get("withUserId");
                    Object upToMsgId = dataMap.get("upToMsgId");
                    if (withUserId != null) {
                        Long withId = Long.valueOf(String.valueOf(withUserId));
                        Long upTo = upToMsgId == null ? null : Long.valueOf(String.valueOf(upToMsgId));
                        interactService.markChatRead(fromUserId, withId, upTo);
                    }
                }
                return;
            }
        } catch (Exception e) {
            log.warn("处理 WebSocket 消息失败，fromUserId: {}, payload: {}", fromUserId, payload, e);
            WsEnvelope<Object> err = WsEnvelope.builder()
                    .type("error")
                    .data("消息格式错误")
                    .build();
            session.sendMessage(new TextMessage(JsonUtil.toJson(err)));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = getUserId(session);
        if (userId != null) {
            interactService.removeChatSession(userId, session);
        }
        log.info("WebSocket 连接关闭，userId: {}, sessionId: {}, status: {}", userId, session.getId(), status);
    }

    private Long getUserId(WebSocketSession session) {
        Object v = session.getAttributes().get(ATTR_USER_ID);
        if (v instanceof Long) {
            return (Long) v;
        }
        if (v instanceof Integer) {
            return ((Integer) v).longValue();
        }
        return null;
    }
}

