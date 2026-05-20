package com.syt.graduationproject.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LikeTargetTypeEnum {

    VIDEO(1, "视频"),

    COMMENT(2, "评论");

    private final Integer code;

    private final String message;
}
