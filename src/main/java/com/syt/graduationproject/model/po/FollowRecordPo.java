package com.syt.graduationproject.model.po;

import lombok.Data;

import java.util.Date;

/**
 * 关注记录表
 */
@Data
public class FollowRecordPo {
    /**
     * 唯一 id
     */
    private Long id;
    
    /**
     * 关注者 ID
     */
    private Long followerId;
    
    /**
     * 被关注者 ID
     */
    private Long followeeId;
    
    /**
     * 状态 0-取消关注 1-有效
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;
}
