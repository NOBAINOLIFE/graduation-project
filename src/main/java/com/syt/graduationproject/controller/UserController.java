package com.syt.graduationproject.controller;

import com.syt.graduationproject.annotation.RequirePermission;
import com.syt.graduationproject.exception.CustomException;
import com.syt.graduationproject.model.request.LoginRequest;
import com.syt.graduationproject.model.request.RegisterRequest;
import com.syt.graduationproject.model.request.UserInfoUpdateRequest;
import com.syt.graduationproject.model.request.UserPasswordUpdateRequest;
import com.syt.graduationproject.model.response.Response;
import com.syt.graduationproject.model.vo.LoginVo;
import com.syt.graduationproject.model.vo.UserCoinChangeLogVo;
import com.syt.graduationproject.model.vo.UserInfoVo;
import com.syt.graduationproject.service.UserService;
import com.syt.graduationproject.util.UserHolderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/graduation-project/user")
@RequirePermission("USER")
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
     * 查询用户信息（userId 为空时查当前登录用户）
     */
    @GetMapping("/info")
    public Response<UserInfoVo> queryUserInfo(@RequestParam(required = false) Long userId) {
        try {
            if (userId == null) {
                userId = UserHolderUtil.getUser().getUserId();
            }
            return Response.success(userService.queryUserInfo(userId));
        } catch (CustomException e) {
            log.warn("查询用户信息失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询用户信息失败，userId：{}", userId, e);
            return Response.fail("查询用户信息失败，系统异常");
        }
    }

    /**
     * 查询当前用户近一周硬币变动记录
     */
    @GetMapping("/coin/change-logs")
    public Response<List<UserCoinChangeLogVo>> queryMyCoinChangeLogs(@RequestParam(required = false) Integer days) {
        try {
            return Response.success(userService.queryMyCoinChangeLogs(days));
        } catch (CustomException e) {
            log.warn("查询硬币变动记录失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询硬币变动记录失败，days：{}", days, e);
            return Response.fail("查询硬币变动记录失败，系统异常");
        }
    }

    /**
     * 更新用户个人信息
     */
    @PostMapping("/update-info")
    public Response<Object> updateUserInfo(@RequestBody UserInfoUpdateRequest request) {
        try {
            Long userId = UserHolderUtil.getUser().getUserId();
            userService.updateUserInfo(userId, request);
            return Response.success();
        } catch (CustomException e) {
            log.warn("更新用户信息失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("更新用户信息失败，request：{}", request, e);
            return Response.fail("更新用户信息失败，系统异常");
        }
    }

    /**
     * 修改用户密码
     */
    @PostMapping("/update-password")
    public Response<Object> updatePassword(@RequestBody UserPasswordUpdateRequest request) {
        try {
            Long userId = UserHolderUtil.getUser().getUserId();
            userService.updatePassword(userId, request);
            return Response.success();
        } catch (CustomException e) {
            log.warn("修改密码失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("修改密码失败，request：{}", request, e);
            return Response.fail("修改密码失败，系统异常");
        }
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Response<Object> logout() {
        Long userId = UserHolderUtil.getUser().getUserId();
        try {
            userService.logout(userId);
            return Response.success();
        } catch (Exception e) {
            log.error("用户登出失败，userId：{}", userId, e);
            return Response.fail("登出失败，系统异常");
        }
    }

    /**
     * 上传头像，返回头像url
     */
    @PostMapping("/upload-avatar")
    public Response<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            String objectName = userService.uploadAvatar(file);
            return Response.success(objectName);
        } catch (Exception e) {
            log.error("上传头像失败", e);
            return Response.fail("上传头像失败");
        }
    }
}
