package com.syt.graduationproject.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReportStatusEnum {

    WAITING_AUDIT(0, "待审核"),

    APPROVED(1, "已通过"),

    REJECTED(2, "已驳回");

    private final Integer code;

    private final String message;
}

