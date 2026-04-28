package com.syt.graduationproject.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentListRequest {

    private Long videoId;

    private Integer sortType;

    /**
     * 用于游标分页场景：上一页的最小热度
     */
    private Long lastHotScore;

    /**
     * 用于游标分页场景：上一页的最小评论时间
     */
    private LocalDateTime lastCreateTime;

    /**
     * 用于游标分页场景：上一页的最小评论id
     */
    private Long lastCommentId;

    private Integer pageSize;
}
