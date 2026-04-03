package com.syt.graduationproject.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 点赞/取消点赞请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeRequest {

    private Long targetId;

    /**
     * 0：视频点赞，2：评论点赞
     */
    private Integer type;

    /**
     * 0：取消点赞，1：点赞
     */
    private Integer operation;
}
