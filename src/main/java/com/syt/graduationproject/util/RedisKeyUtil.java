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

    /**
     * JWT 旧令牌短期兼容：jwt:whitelist:previous:{userId} -> token
     */
    public static String jwtPreviousWhitelistKey(Long userId) {
        return "jwt:whitelist:previous:" + userId;
    }

    /**
     * 分片上传会话：video:upload:session:{videoId}:{uploadToken}
     */
    public static String videoUploadSessionKey(Long videoId, String uploadToken) {
        return "video:upload:session:" + videoId + ":" + uploadToken;
    }

    /**
     * 分片映射：video:upload:parts:{videoId}:{uploadToken} -> hash(chunkIndex -> objectName)
     */
    public static String videoUploadPartsKey(Long videoId, String uploadToken) {
        return "video:upload:parts:" + videoId + ":" + uploadToken;
    }

    /**
     * 转码互斥锁：video:transcode:lock:{videoId}
     */
    public static String videoTranscodeLockKey(Long videoId) {
        return "video:transcode:lock:" + videoId;
    }

    /**
     * 播放计数去重 key：video:pv:dedup:{videoId}:{userId}
     */
    public static String videoPlayPvDedupKey(Long videoId, Long userId) {
        return "video:pv:dedup:" + videoId + ":" + userId;
    }

    /**
     * 播放量增量 hash：video:pv:delta -> field(videoId)=delta
     */
    public static String videoPlayPvDeltaKey() {
        return "video:pv:delta";
    }
}

