-- Admin module migration
-- 1) Role tables and seed data
CREATE TABLE IF NOT EXISTS tb_role
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_name   VARCHAR(32) NOT NULL,
    role_desc   VARCHAR(128) DEFAULT NULL,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_name (role_name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS tb_user_role
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    role_id     BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_id (user_id),
    KEY idx_role_id (role_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

INSERT INTO tb_role (id, role_name, role_desc)
VALUES (1, 'USER', '普通用户'),
       (2, 'ADMIN', '管理员')
ON DUPLICATE KEY UPDATE role_desc   = VALUES(role_desc),
                        update_time = CURRENT_TIMESTAMP;

-- 2) Backfill existing users to USER role
INSERT INTO tb_user_role (user_id, role_id)
SELECT u.id, 1
FROM tb_user u
         LEFT JOIN tb_user_role ur ON ur.user_id = u.id
WHERE ur.user_id IS NULL;

-- 3) Report table
CREATE TABLE IF NOT EXISTS tb_report
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    reporter_id BIGINT       NOT NULL,
    target_type TINYINT      NOT NULL COMMENT '1-user 2-video',
    target_id   BIGINT       NOT NULL,
    reason      VARCHAR(256) NOT NULL,
    detail      VARCHAR(1024)         DEFAULT NULL,
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0-waiting 1-approved 2-rejected',
    reviewer_id BIGINT                DEFAULT NULL,
    review_note VARCHAR(512)          DEFAULT NULL,
    create_time TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_reporter_id (reporter_id),
    KEY idx_target (target_type, target_id),
    KEY idx_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- 4) Video status code migration reminder
-- New status map:
-- 0 UPLOADING, 1 UPLOAD_SUCCESS, 2 WAITING_TRANSCODE, 3 TRANSCODING,
-- 4 TRANSCODE_SUCCESS, 5 TRANSCODE_FAILED, 6 AUDITING, 7 AUDIT_PASSED,
-- 8 AUDIT_REJECTED, 9 PUBLISHED, 10 DELETED, 11 BANNED

-- 5) Video audit record table
CREATE TABLE IF NOT EXISTS tb_video_audit_record
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    video_id     BIGINT  NOT NULL,
    applicant_id BIGINT  NOT NULL,
    status       TINYINT NOT NULL DEFAULT 0 COMMENT '0-auditing 1-passed 2-rejected',
    reviewer_id  BIGINT           DEFAULT NULL,
    review_note  VARCHAR(512)     DEFAULT NULL,
    create_time  TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_video_id (video_id),
    KEY idx_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

