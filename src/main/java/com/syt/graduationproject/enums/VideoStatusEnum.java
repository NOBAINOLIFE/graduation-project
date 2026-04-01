package com.syt.graduationproject.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VideoStatusEnum {

    NORMAL(0, "正常"),

    AUDITING(1, "审核中"),

    BANNED(2, "封禁"),

    DELETED(3, "删除");

    private final int code;

    private final String message;
}
