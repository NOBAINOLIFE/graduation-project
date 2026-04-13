package com.syt.graduationproject.model.request;

import lombok.Data;

@Data
public class UserPasswordUpdateRequest {
    private String oldPassword;
    private String newPassword;
}

