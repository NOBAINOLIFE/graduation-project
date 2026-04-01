package com.syt.graduationproject.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatusEnum {

    NORMAL(0, "正常"),

    BANNED(1, "封禁"),

    DELETED(2, "删除");

    private final int code;

    private final String message;
}
