CREATE TABLE IF NOT EXISTS tb_user_coin_change_log
(
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id           BIGINT  NOT NULL,
    change_amount     INT     NOT NULL COMMENT '正数为增加，负数为减少',
    change_type       TINYINT NOT NULL COMMENT '1-每日登录奖励 2-给视频投币',
    related_target_id BIGINT           DEFAULT NULL COMMENT '关联对象ID，例如视频ID',
    create_time       TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_time (user_id, create_time),
    KEY idx_user_type (user_id, change_type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
