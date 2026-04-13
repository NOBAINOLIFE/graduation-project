package com.syt.graduationproject.model.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 私信消息表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_private_message")
public class PrivateMessagePo {

    /**
     * 主键ID（服务端消息ID）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发送方用户ID
     */
    @TableField("from_user_id")
    private Long fromUserId;

    /**
     * 接收方用户ID
     */
    @TableField("to_user_id")
    private Long toUserId;

    /**
     * 前端生成的消息ID（幂等/对齐用，可为空）
     */
    @TableField("client_msg_id")
    private String clientMsgId;

    /**
     * 文本内容
     */
    @TableField("content")
    private String content;

    /**
     * 消息状态：0-SAVED 1-DELIVERED 2-ACKED 3-FAIL
     */
    @TableField("status")
    private Integer status;

    /**
     * 投递时间（服务端向对方在线会话发送成功的时间）
     */
    @TableField("delivered_time")
    private LocalDateTime deliveredTime;

    /**
     * 确认时间（客户端 ack 的时间）
     */
    @TableField("acked_time")
    private LocalDateTime ackedTime;

    /**
     * 已读时间（接收方标记 read 的时间）
     */
    @TableField("read_time")
    private LocalDateTime readTime;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
}

