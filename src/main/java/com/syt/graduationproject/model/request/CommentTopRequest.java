package com.syt.graduationproject.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentTopRequest {

    /**
     * 评论ID
     */
    private Long commentId;

    /**
     * 1: 置顶, 0: 取消置顶
     */
    private Integer operation;
}
