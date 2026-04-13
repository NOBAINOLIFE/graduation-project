package com.syt.graduationproject.websocket;

/**
 * 私聊 Redis Key 规范
 */
public class ChatRedisKeys {

    private ChatRedisKeys() {
    }

    /**
     * 在线映射：chat:online:{userId} -> sessionId
     */
    public static String onlineKey(Long userId) {
        return "chat:online:" + userId;
    }

    /**
     * 离线消息队列：chat:offline:{userId} -> List(JSON)
     */
    public static String offlineListKey(Long userId) {
        return "chat:offline:" + userId;
    }
}

