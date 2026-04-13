-- 私信消息表
CREATE TABLE IF NOT EXISTS `tb_private_message`
(
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID（服务端消息ID）',
    `from_user_id`   BIGINT        NOT NULL COMMENT '发送方用户ID',
    `to_user_id`     BIGINT        NOT NULL COMMENT '接收方用户ID',
    `client_msg_id`  VARCHAR(64)            DEFAULT NULL COMMENT '前端消息ID（幂等/回执关联）',
    `content`        VARCHAR(1024) NOT NULL COMMENT '文本内容',
    `status`         TINYINT       NOT NULL DEFAULT 0 COMMENT '状态：0-SAVED 1-DELIVERED 2-ACKED 3-FAIL',
    `delivered_time` TIMESTAMP              DEFAULT NULL COMMENT '投递时间',
    `acked_time`     TIMESTAMP              DEFAULT NULL COMMENT '确认时间',
    `create_time`    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_to_user_time` (`to_user_id`, `create_time`),
    KEY `idx_from_user_time` (`from_user_id`, `create_time`),
    UNIQUE KEY `uk_from_client_msg` (`from_user_id`, `client_msg_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='私信消息表';

