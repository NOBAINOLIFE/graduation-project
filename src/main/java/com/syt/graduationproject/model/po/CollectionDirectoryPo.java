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
 * 收藏夹目录表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_collection_directory")
public class CollectionDirectoryPo {

    /**
     * 自增ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 收藏夹名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 描述
     */
    @TableField(value = "description")
    private String description;

    /**
     * 封面图
     */
    @TableField(value = "cover_url")
    private String coverUrl;

    /**
     * 是否公开：0-私有，1-公开
     */
    @TableField(value = "is_public")
    private Integer isPublic;

    /**
     * 是否默认收藏夹：0-否，1-是
     */
    @TableField(value = "is_default")
    private Integer isDefault;

    /**
     * 状态 0-未删除 1-已删除
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private LocalDateTime updateTime;
}