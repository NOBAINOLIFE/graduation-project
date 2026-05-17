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
 * 二级评论表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_comment")
public class CommentPo {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属内容ID（如视频ID）
     */
    @TableField(value = "video_id")
    private Long videoId;

    /**
     * 评论者ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 根评论ID：0表示自己是根评论，非0表示属于哪条主评论
     */
    @TableField(value = "root_id")
    private Long rootId;

    /**
     * 父评论ID：回复的是哪一条具体评论
     */
    @TableField(value = "parent_id")
    private Long parentId;

    /**
     * 被回复者ID（冗余，方便显示：回复 @xxx）
     */
    @TableField(value = "reply_user_id")
    private Long replyUserId;

    /**
     * 评论内容
     */
    @TableField(value = "content")
    private String content;

    /**
     * 手动逻辑删除标志
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    /**
     * 是否置顶：1-置顶，0-未置顶
     */
    @TableField(value = "is_top")
    private Integer isTop;

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
