package com.syt.graduationproject.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VideoResolutionEnum {

    LOW(0, "流畅"),

    MEDIUM(1, "标清"),

    HIGH(2, "高清"),

    SUPER(3, "超清"),

    ORIGINAL(4, "原画");

    private final int code;

    private final String message;
}
