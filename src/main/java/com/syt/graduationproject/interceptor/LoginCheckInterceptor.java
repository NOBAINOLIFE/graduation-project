package com.syt.graduationproject.interceptor;

import com.syt.graduationproject.annotation.RequirePermission;
import com.syt.graduationproject.enums.RoleEnum;
import com.syt.graduationproject.model.dto.UserDto;
import com.syt.graduationproject.model.response.Response;
import com.syt.graduationproject.util.*;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static com.syt.graduationproject.constant.UserConstant.ADMIN_PERMISSION;
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

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 若url中含有login或register则放行
        String uri = request.getRequestURI();
        if (uri.equals("/graduation-project/user/login") || uri.equals("/graduation-project/user/register")) {
            return true;
        }

        // 放行预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 获取请求头中的令牌，若不存在，返回错误
        String jwtToken = request.getHeader(JWT_HTTP_HEADER);
        if (StringUtils.isEmpty(jwtToken)) {
            log.error("请求头token为空，返回未登录的信息");
            editResponseMessage(response, "未登录");
            return false;
        }

        // 若存在，则解析token，若解析失败，返回错误
        try {
            UserDto userDto = praseJwtToken(jwtToken);
            // 校验token是否在Redis白名单
            if (!checkWhitelist(jwtToken, userDto.getUserId())) {
                log.error("token未在白名单，疑似已登出或失效");
                editResponseMessage(response, "未登录或已失效");
                return false;
            }

            if (RoleEnum.ADMIN.getRoleCode().equalsIgnoreCase(userDto.getRoleCode())
                    && !isManagerAllowedPath(uri)) {
                editResponseMessage(response, "管理员仅可访问后台管理接口");
                return false;
            }

            if (!hasPermission(handler, userDto)) {
                editResponseMessage(response, "无权限访问");
                return false;
            }

            UserHolderUtil.saveUser(userDto);
        } catch (Exception e) {
            log.error("解析JWT令牌失败", e);
            editResponseMessage(response, "登录失败");
            return false;
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) {
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
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
    private UserDto praseJwtToken(String jwtToken) {
        Claims claims = JwtUtil.parseJwtToken(jwtToken);
        return UserDto.builder()
                .userId(claims.get(USER_ID, Long.class))
                .username(claims.get(USERNAME, String.class))
                .roleId(claims.get(ROLE_ID, Long.class))
                .roleCode(claims.get(ROLE_CODE, String.class))
                .build();
    }

    private boolean isManagerAllowedPath(String uri) {
        return uri.startsWith("/graduation-project/manager") || uri.equals("/graduation-project/user/logout");
    }

    private boolean hasPermission(Object handler, UserDto userDto) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequirePermission requirePermission = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (requirePermission == null) {
            return true;
        }
        if (ADMIN_PERMISSION.equalsIgnoreCase(requirePermission.value())) {
            return RoleEnum.ADMIN.getRoleCode().equalsIgnoreCase(userDto.getRoleCode());
        }
        return true;
    }

    /**
     * 校验 token 是否在 Redis 白名单
     */
    private boolean checkWhitelist(String jwtToken, Long userId) {
        String whitelistToken = stringRedisTemplate.opsForValue().get(RedisKeyUtil.jwtWhitelistKey(userId));
        return !StringUtils.isEmpty(whitelistToken) && whitelistToken.equals(jwtToken);
    }
}
