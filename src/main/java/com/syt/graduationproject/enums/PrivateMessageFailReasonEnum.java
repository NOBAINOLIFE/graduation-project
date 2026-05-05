package com.syt.graduationproject.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 私信发送失败原因
 */
@Getter
@AllArgsConstructor
public enum PrivateMessageFailReasonEnum {

    /**
     * 未失败
     */
    NONE(0, "无"),

    /**
     * 系统异常
     */
    SYSTEM_ERROR(1, "系统异常"),

    /**
     * 发送方被封禁
     */
    SENDER_BANNED(2, "发送方被封禁"),

    /**
     * 接收方被封禁
     */
    RECEIVER_BANNED(3, "接收方被封禁"),

    /**
     * 发送方拉黑了接收方
     */
    SENDER_BLOCKED_RECEIVER(4, "已拉黑对方"),

    /**
     * 接收方拉黑了发送方
     */
    RECEIVER_BLOCKED_SENDER(5, "对方已拉黑你");

    private final int code;

    private final String message;
}