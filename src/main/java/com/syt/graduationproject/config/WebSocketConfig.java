package com.syt.graduationproject.config;

import com.syt.graduationproject.interceptor.ChatHandshakeInterceptor;
import com.syt.graduationproject.websocket.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import static com.syt.graduationproject.constant.ChatWsConstant.WS_PATH;

/**
 * WebSocket 配置
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, WS_PATH)
                .addInterceptors(new ChatHandshakeInterceptor())
                .setAllowedOrigins("*");
    }
}

