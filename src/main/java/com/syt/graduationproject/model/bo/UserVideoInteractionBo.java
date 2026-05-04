package com.syt.graduationproject.model.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户视频交互信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVideoInteractionBo {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 视频ID
     */
    private Long videoId;

    /**
     * 是否点赞
     */
    private Boolean isLike;

    /**
     * 是否收藏
     */
    private Boolean isCollect;

    /**
     * 投币数量
     */
    private Integer coinCount;
}
