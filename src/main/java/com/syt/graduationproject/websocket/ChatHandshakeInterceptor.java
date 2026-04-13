package com.syt.graduationproject.websocket;

import com.syt.graduationproject.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.syt.graduationproject.constant.UserConstant.USER_ID;
import static com.syt.graduationproject.websocket.ChatWsConstants.ATTR_USER_ID;

/**
 * WebSocket 握手拦截器：从 token 中解析 userId 写入 session attributes
 */
@Slf4j
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    private static final String TOKEN_HEADER = "token";
    private static final String TOKEN_PARAM = "token";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        try {
            String token = null;
            if (request instanceof ServletServerHttpRequest) {
                HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
                token = servletRequest.getHeader(TOKEN_HEADER);
                if (StringUtils.isBlank(token)) {
                    token = servletRequest.getParameter(TOKEN_PARAM);
                }
                if (StringUtils.isBlank(token)) {
                    // 兜底：从 query string 自己解析一次
                    String query = servletRequest.getQueryString();
                    token = extractQueryParam(query, TOKEN_PARAM);
                }
            }
            if (StringUtils.isBlank(token)) {
                log.warn("WebSocket 握手失败：token 缺失");
                return false;
            }
            Claims claims = JwtUtil.parseJwtToken(token);
            Long userId = claims.get(USER_ID, Long.class);
            if (userId == null) {
                log.warn("WebSocket 握手失败：token 中 userId 为空");
                return false;
            }
            attributes.put(ATTR_USER_ID, userId);
            return true;
        } catch (Exception e) {
            log.warn("WebSocket 握手失败：解析 token 异常", e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
    }

    private String extractQueryParam(String query, String key) {
        if (StringUtils.isBlank(query) || StringUtils.isBlank(key)) {
            return null;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String k = pair.substring(0, idx);
            if (!key.equals(k)) {
                continue;
            }
            String v = pair.substring(idx + 1);
            try {
                return URLDecoder.decode(v, StandardCharsets.UTF_8.name());
            } catch (Exception ignore) {
                return v;
            }
        }
        return null;
    }
}

