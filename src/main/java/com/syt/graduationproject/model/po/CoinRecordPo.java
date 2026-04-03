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
 * 视频投币记录表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_coin_record")
public class CoinRecordPo {

    /**
     * 唯一id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 投币人ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 视频ID
     */
    @TableField(value = "video_id")
    private Long videoId;

    /**
     * 投币数量（通常上限为2）
     */
    @TableField(value = "amount")
    private Integer amount;

    /**
     * 投币时间
     */
    @TableField(value = "create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private LocalDateTime updateTime;
}