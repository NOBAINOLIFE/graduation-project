package com.syt.graduationproject.websocket;

import com.syt.graduationproject.service.impl.InteractServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 心跳超时清理（简单实现）：定时关闭长时间无心跳的 WebSocket 会话
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ChatSessionCleaner {

    private final InteractServiceImpl interactService;

    /**
     * 30 秒扫描一次，超过 90 秒无心跳视为超时
     */
    @Scheduled(fixedDelay = 30_000)
    public void cleanup() {
        try {
            interactService.closeIdleChatSessions(90_000L);
        } catch (Exception e) {
            log.warn("清理超时 WebSocket 会话失败", e);
        }
    }
}

