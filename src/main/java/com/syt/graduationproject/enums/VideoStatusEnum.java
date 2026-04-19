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

    AUDITING(6, "审核中"),

    AUDIT_PASSED(7, "审核通过"),

    AUDIT_REJECTED(8, "审核未通过"),

    PUBLISHED(9, "已发布"),

    DELETED(10, "已删除"),

    BANNED(11, "已封禁");

    private final int code;

    private final String message;
}
