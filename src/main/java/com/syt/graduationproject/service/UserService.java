package com.syt.graduationproject.service;

import com.syt.graduationproject.model.request.LoginRequest;
import com.syt.graduationproject.model.request.RegisterRequest;
import com.syt.graduationproject.model.request.UserInfoUpdateRequest;
import com.syt.graduationproject.model.request.UserPasswordUpdateRequest;
import com.syt.graduationproject.model.vo.LoginVo;
import com.syt.graduationproject.model.vo.UserInfoVo;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    @Transactional(rollbackFor = Exception.class)
    void register(RegisterRequest request);

    LoginVo login(LoginRequest request);

    UserInfoVo queryUserInfo(Long userId);

    void updateUserInfo(Long userId, UserInfoUpdateRequest request);

    void updatePassword(Long userId, UserPasswordUpdateRequest request);

    String uploadAvatar(MultipartFile file);
}
