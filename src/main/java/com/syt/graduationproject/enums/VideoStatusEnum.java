package com.syt.graduationproject.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VideoStatusEnum {

    WAITING_UPLOAD(0, "待上传"),

    UPLOADED(1, "上传完成"),

    TRANSCODING(2, "转码中"),

    AUDITING(3, "审核中"),

    NORMAL(4, "正常"),

    BANNED(5, "封禁"),

    DELETED(6, "删除");

    private final int code;

    private final String message;
}
