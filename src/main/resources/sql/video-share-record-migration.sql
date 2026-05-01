CREATE TABLE IF NOT EXISTS tb_video_share_record
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    video_id    BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_video_user_share (video_id, user_id),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
