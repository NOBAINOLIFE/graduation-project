package com.syt.graduationproject.controller;

import com.syt.graduationproject.exception.CustomException;
import com.syt.graduationproject.model.request.LoginRequest;
import com.syt.graduationproject.model.request.RegisterRequest;
import com.syt.graduationproject.model.response.Response;
import com.syt.graduationproject.model.vo.LoginVo;
import com.syt.graduationproject.model.vo.UserInfoVo;
import com.syt.graduationproject.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/graduation-project/user")
public class UserController {

    private final UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Response<Object> register(@RequestBody RegisterRequest request) {
        try {
            userService.register(request);
            return Response.success();
        } catch (CustomException e) {
            log.warn("用户注册失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("用户注册失败，registerRequest：{}", request, e);
            return Response.fail("注册失败，系统异常");
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Response<LoginVo> login(@RequestBody LoginRequest request) {
        try {
            return Response.success(userService.login(request));
        } catch (CustomException e) {
            log.warn("用户登录失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("用户登录失败，loginRequest：{}", request, e);
            return Response.fail("登录失败，系统异常");
        }
    }

    /**
     * 查询用户信息
     */
    @GetMapping("/info")
    public Response<UserInfoVo> queryUserInfo(@RequestParam Long userId) {
        try {
            return Response.success(userService.queryUserInfo(userId));
        } catch (CustomException e) {
            log.warn("查询用户信息失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询用户信息失败，userId：{}", userId, e);
            return Response.fail("查询用户信息失败，系统异常");
        }
    }
}
