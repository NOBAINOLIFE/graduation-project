package com.syt.graduationproject.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 私信消息展示对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivateMessageVo {

    private Long id;

    private Long fromUserId;

    private Long toUserId;

    private String content;

    private Integer status;

    private Integer failReason;

    private String failReasonText;

    private LocalDateTime createTime;

    private LocalDateTime deliveredTime;

    private LocalDateTime ackedTime;

    private LocalDateTime readTime;
}

