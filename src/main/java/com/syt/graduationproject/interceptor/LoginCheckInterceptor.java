package com.syt.graduationproject.interceptor;

import com.syt.graduationproject.model.dto.UserDto;
import com.syt.graduationproject.model.response.Response;
import com.syt.graduationproject.util.JsonUtil;
import com.syt.graduationproject.util.JwtUtil;
import com.syt.graduationproject.util.RedisJwtWhitelistUtil;
import com.syt.graduationproject.util.UserHolderUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.syt.graduationproject.constant.UserConstant.USERNAME;
import static com.syt.graduationproject.constant.UserConstant.USER_ID;

@Slf4j
@Component
public class LoginCheckInterceptor implements HandlerInterceptor {

    private static final String JWT_HTTP_HEADER = "token";

    @Autowired
    private RedisJwtWhitelistUtil redisJwtWhitelistUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 若url中含有login或register则放行
        String uri = request.getRequestURI();
        if (uri.equals("/graduation-project/user/login") || uri.equals("/graduation-project/user/register")) {
            return true;
        }

        // 获取请求头中的令牌，若不存在，返回错误
        String jwtToken = request.getHeader(JWT_HTTP_HEADER);
        if (StringUtils.isEmpty(jwtToken)) {
            log.error("请求头token为空，返回未登录的信息");
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JsonUtil.toJson(Response.fail("未登录")));
            return false;
        }
        // 校验token是否在Redis白名单
        if (!redisJwtWhitelistUtil.isTokenWhitelisted(jwtToken)) {
            log.error("token未在白名单，疑似已登出或失效");
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JsonUtil.toJson(Response.fail("未登录或已失效")));
            return false;
        }

        // 若存在，则解析token，若解析失败，返回错误
        try {
            Claims claims = JwtUtil.parseJwtToken(jwtToken);
            UserDto userDto = UserDto.builder()
                    .userId(claims.get(USER_ID, Long.class))
                    .username(claims.get(USERNAME, String.class))
                    .build();
            UserHolderUtil.saveUser(userDto);
        } catch (Exception e) {
            log.error("解析JWT令牌失败", e);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JsonUtil.toJson(Response.fail("登录失败")));
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
}
