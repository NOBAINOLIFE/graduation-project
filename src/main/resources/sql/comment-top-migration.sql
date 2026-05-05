-- 评论置顶字段
ALTER TABLE tb_comment
    ADD COLUMN is_top TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶：1-置顶，0-未置顶' AFTER content;

CREATE INDEX idx_comment_video_root_top
    ON tb_comment (video_id, root_id, is_top, is_deleted, create_time, id);