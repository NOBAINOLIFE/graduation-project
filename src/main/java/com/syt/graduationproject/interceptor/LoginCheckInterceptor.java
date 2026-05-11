package com.syt.graduationproject.interceptor;

import com.syt.graduationproject.model.dto.UserDto;
import com.syt.graduationproject.model.response.Response;
import com.syt.graduationproject.util.*;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.syt.graduationproject.constant.UserConstant.ROLE_CODE;
import static com.syt.graduationproject.constant.UserConstant.ROLE_ID;
import static com.syt.graduationproject.constant.UserConstant.USERNAME;
import static com.syt.graduationproject.constant.UserConstant.USER_ID;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginCheckInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String JWT_HTTP_HEADER = "token";
    private static final String REFRESHED_TOKEN_HEADER = "x-access-token";
    private static final long REFRESH_THRESHOLD_MILLIS = 30 * 60 * 1000L;
    private static final long PREVIOUS_TOKEN_GRACE_MILLIS = 5 * 60 * 1000L;

    @Override
    public boolean preHandle(HttpServletRequest request, @NotNull HttpServletResponse response,
                             @NotNull Object handler) throws Exception {
        String uri = request.getRequestURI();

        // 放行预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 公开路径 + 没有 token：直接放行
        String jwtToken = request.getHeader(JWT_HTTP_HEADER);
        if (isPublicPath(uri) && StringUtils.isEmpty(jwtToken)) {
            return true;
        }

        // 非公开路径 + 没有 token：拦截
        if (!isPublicPath(uri) && StringUtils.isEmpty(jwtToken)) {
            log.error("请求头token为空，返回未登录的信息");
            editResponseMessage(response, "未登录");
            return false;
        }

        // 有 token：解析 token
        try {
            Claims claims = JwtUtil.parseJwtToken(jwtToken);
            UserDto userDto = buildUserDto(claims);
            // 校验token是否在Redis白名单
            if (!checkWhitelist(jwtToken, userDto.getUserId())) {
                log.error("token未在白名单，疑似已登出或失效");
                editResponseMessage(response, "未登录或已失效");
                return false;
            }

            refreshTokenIfNecessary(response, jwtToken, claims, userDto.getUserId());
            UserHolderUtil.saveUser(userDto);
        } catch (Exception e) {
            log.error("解析JWT令牌失败", e);
            editResponseMessage(response, "操作失败，登录令牌错误");
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response,
                                @NotNull Object handler, Exception ex) {
        UserHolderUtil.removeUser();
    }

    /**
     * 自定义错误 response 消息内容
     */
    private void editResponseMessage(HttpServletResponse response, String message) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JsonUtil.toJson(Response.fail(message)));
    }

    /**
     * 解析 token
     */
    private UserDto buildUserDto(Claims claims) {
        return UserDto.builder()
                .userId(claims.get(USER_ID, Long.class))
                .username(claims.get(USERNAME, String.class))
                .roleId(claims.get(ROLE_ID, Long.class))
                .roleCode(claims.get(ROLE_CODE, String.class))
                .build();
    }

    private boolean isPublicPath(String uri) {
        return uri.equals("/graduation-project/manager/login")
                || uri.equals("/graduation-project/user/login")
                || uri.equals("/graduation-project/user/register")
                || uri.equals("/graduation-project/video/partitions")
                || uri.equals("/graduation-project/video/videoPlayList")
                || uri.startsWith("/graduation-project/video/videoInfo/")
                || uri.equals("/graduation-project/interact/comment/list")
                || uri.equals("/graduation-project/search/video");
    }

    /**
     * 校验 token 是否在 Redis 白名单
     */
    private boolean checkWhitelist(String jwtToken, Long userId) {
        String whitelistToken = stringRedisTemplate.opsForValue().get(RedisKeyUtil.jwtWhitelistKey(userId));
        if (!StringUtils.isEmpty(whitelistToken) && whitelistToken.equals(jwtToken)) {
            return true;
        }
        String previousToken = stringRedisTemplate.opsForValue().get(RedisKeyUtil.jwtPreviousWhitelistKey(userId));
        return !StringUtils.isEmpty(previousToken) && previousToken.equals(jwtToken);
    }

    private void refreshTokenIfNecessary(HttpServletResponse response, String oldToken, Claims claims, Long userId) {
        if (StringUtils.isBlank(oldToken) || claims == null || userId == null) {
            return;
        }

        String whitelistKey = RedisKeyUtil.jwtWhitelistKey(userId);
        String currentToken = stringRedisTemplate.opsForValue().get(whitelistKey);
        if (StringUtils.isBlank(currentToken)) {
            return;
        }

        if (!StringUtils.equals(oldToken, currentToken)) {
            response.setHeader(REFRESHED_TOKEN_HEADER, currentToken);
            return;
        }

        Date expiration = claims.getExpiration();
        if (expiration == null) {
            return;
        }
        long remainingMillis = expiration.getTime() - System.currentTimeMillis();
        if (remainingMillis > REFRESH_THRESHOLD_MILLIS) {
            return;
        }

        String newToken = generateRefreshedToken(claims);
        stringRedisTemplate.opsForValue().set(
                RedisKeyUtil.jwtPreviousWhitelistKey(userId),
                oldToken,
                PREVIOUS_TOKEN_GRACE_MILLIS,
                TimeUnit.MILLISECONDS
        );
        stringRedisTemplate.opsForValue().set(
                whitelistKey,
                newToken,
                JwtUtil.EXPIRATION,
                TimeUnit.MILLISECONDS
        );
        response.setHeader(REFRESHED_TOKEN_HEADER, newToken);
    }

    private String generateRefreshedToken(Claims claims) {
        Map<String, Object> claimMap = new HashMap<>();
        claimMap.put(USER_ID, claims.get(USER_ID, Long.class));
        claimMap.put(USERNAME, claims.get(USERNAME, String.class));
        claimMap.put(ROLE_CODE, claims.get(ROLE_CODE, String.class));
        Long roleId = claims.get(ROLE_ID, Long.class);
        if (roleId != null) {
            claimMap.put(ROLE_ID, roleId);
        }
        return JwtUtil.generateJwtToken(claimMap);
    }
}
