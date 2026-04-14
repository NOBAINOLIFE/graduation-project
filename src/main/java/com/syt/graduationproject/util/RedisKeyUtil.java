package com.syt.graduationproject.util;

import lombok.Data;

/**
 * 私聊 Redis Key 规范
 */
@Data
public class RedisKeyUtil {

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

    /**
     * JWT 白名单：jwt:whitelist:{userId} -> token
     */
    public static String jwtWhitelistKey(Long userId) {
        return "jwt:whitelist:" + userId;
    }
}

