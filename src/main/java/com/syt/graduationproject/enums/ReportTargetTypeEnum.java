package com.syt.graduationproject.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReportTargetTypeEnum {

    USER(1, "用户"),

    VIDEO(2, "视频"),

    COMMENT(3, "评论");

    private final Integer code;

    private final String message;
}

