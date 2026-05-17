package com.syt.graduationproject.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CoinChangeTypeEnum {

    DAILY_REWARD(1, "每日登录奖励"),

    VIDEO_REWARD(2, "给视频打赏");

    private final Integer code;

    private final String message;
}
