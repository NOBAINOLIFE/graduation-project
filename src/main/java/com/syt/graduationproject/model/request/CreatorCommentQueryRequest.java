package com.syt.graduationproject.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatorCommentQueryRequest {

    private String keyword;

    private Long videoId;

    /**
     * 1-最新评论 2-最多点赞
     */
    private Integer sortType;

    private Integer pageNum;

    private Integer pageSize;
}
