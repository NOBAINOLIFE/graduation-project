package com.syt.graduationproject.model.vo.Page;

import com.syt.graduationproject.model.vo.CommentVo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CommentPageVo extends PageVo<CommentVo>{

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
}
