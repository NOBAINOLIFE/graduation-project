package com.syt.graduationproject.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VideoResolutionEnum {

    ORIGINAL(0, "原视频", "original"),

//    LOW(1, "流畅", "360"),

    MEDIUM(2, "标清", "480"),

    HIGH(3, "高清", "720"),

    SUPER(4, "超清", "1080"),

    MASTER(5, "自适应", "master");

    private final int code;

    private final String name;

    private final String resolution;

    public static VideoResolutionEnum fromCode(int code) {
        for (VideoResolutionEnum value : VideoResolutionEnum.values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid VideoResolutionEnum code: " + code);
    }
}
