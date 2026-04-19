package com.syt.graduationproject.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VideoAuditRecordStatusEnum {

    AUDITING(0, "审核中"),

    PASSED(1, "审核通过"),

    REJECTED(2, "审核驳回");

    private final Integer code;

    private final String message;
}

