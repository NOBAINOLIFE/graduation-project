package com.syt.graduationproject.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VideoResolutionEnum {

//    LOW(0, "流畅", "270p"),

//    MEDIUM(1, "标清", "360p"),

    HIGH(2, "高清", "720p"),

    SUPER(3, "超清", "1080p"),

    ORIGINAL(4, "原画", "original"),

    MASTER(5, "自适应", "master");

    private final int code;

    private final String name;

    private final String resolution;
}
