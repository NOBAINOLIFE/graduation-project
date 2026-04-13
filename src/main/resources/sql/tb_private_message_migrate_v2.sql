-- 私信消息表升级：增加 read_time 字段
ALTER TABLE `tb_private_message`
  ADD COLUMN `read_time` TIMESTAMP NULL COMMENT '已读时间' AFTER `acked_time`;

-- （可选）如果你希望对 READ 做索引，放开下面这行：
-- CREATE INDEX `idx_to_user_read` ON `tb_private_message`(`to_user_id`,`read_time`);

