package com.syt.graduationproject.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    private Long account;

    private String password;

    /**
     * 是否管理员登录：true-管理员后台登录，false/null-用户端登录
     */
    private Boolean isAdminLogin;
}
