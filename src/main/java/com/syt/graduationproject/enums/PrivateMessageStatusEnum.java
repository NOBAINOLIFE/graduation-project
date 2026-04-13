package com.syt.graduationproject.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 私信消息状态
 */
@Getter
@AllArgsConstructor
public enum PrivateMessageStatusEnum {

    /**
     * 已保存（服务端落库完成）
     */
    SAVED(0, "已保存"),

    /**
     * 已投递（至少投递到对方一个在线会话）
     */
    DELIVERED(1, "已投递"),

    /**
     * 已确认（客户端收到后回 ack）
     */
    ACKED(2, "已确认"),

    /**
     * 发送失败/异常
     */
    FAIL(3, "失败"),

    /**
     * 已读（接收方在会话中标记 read）
     */
    READ(4, "已读");

    private final int code;

    private final String message;
}

