package com.syt.graduationproject.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCodeEnum {

    SUCCESS(0, "请求成功"),

    FAIL(1, "请求失败");

    private final int code;

    private final String message;
}
