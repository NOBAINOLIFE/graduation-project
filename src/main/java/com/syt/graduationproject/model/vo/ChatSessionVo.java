package com.syt.graduationproject.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话列表项（按双方 userId 直接从消息表聚合）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionVo {

    /**
     * 对方用户ID
     */
    private Long withUserId;

    /**
     * 对方用户名
     */
    private String withUsername;

    /**
     * 对方头像
     */
    private String withAvatarUrl;

    /**
     * 最后一条消息ID
     */
    private Long lastMsgId;

    /**
     * 最后一条消息内容
     */
    private String lastContent;

    /**
     * 最后一条消息时间
     */
    private LocalDateTime lastTime;

    /**
     * 未读数（对我而言）
     */
    private Long unreadCount;
}

