package com.syt.graduationproject.service;

import com.syt.graduationproject.model.request.LoginRequest;
import com.syt.graduationproject.model.request.RegisterRequest;
import com.syt.graduationproject.model.vo.LoginVo;

public interface UserService {

    void register(RegisterRequest request);

    LoginVo login(LoginRequest request);
}
