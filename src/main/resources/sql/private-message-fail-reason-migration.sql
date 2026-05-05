ALTER TABLE tb_private_message
    ADD COLUMN fail_reason TINYINT NOT NULL DEFAULT 0 COMMENT '失败原因：0-无 1-系统异常 2-发送方被封禁 3-接收方被封禁 4-发送方拉黑接收方 5-接收方拉黑发送方' AFTER status;
