package com.syt.graduationproject.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VideoStatusEnum {

    UPLOADING(0, "上传中"),

    UPLOAD_SUCCESS(1, "上传成功"),

    WAITING_TRANSCODE(2, "等待转码"),

    TRANSCODING(3, "转码中"),

    TRANSCODE_SUCCESS(4, "转码成功"),

    TRANSCODE_FAILED(5, "转码失败"),

    PUBLISHED(6, "已发布"),

    DELETED(7, "已删除");

    private final int code;

    private final String message;
}
