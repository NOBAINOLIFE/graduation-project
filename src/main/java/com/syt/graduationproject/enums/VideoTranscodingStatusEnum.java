package com.syt.graduationproject.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VideoTranscodingStatusEnum {

    WAITING_TRANSCODING(0, "待转码"),

    TRANSCODING(1, "转码中"),

    TRANSCODING_SUCCESS(2, "转码成功"),

    TRANSCODING_FAIL(3, "转码失败");

    private final int code;

    private final String message;
}
