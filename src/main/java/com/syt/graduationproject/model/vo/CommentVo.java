package com.syt.graduationproject.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentVo {

    private Long commentId;

    private Long userId;

    private String username;

    private String avatarUrl;

    private String content;

    private Long rootId;

    private Long parentId;

    private Long replyUserId;

    private String replyUsername;

    private String replyUserAvatarUrl;

    private Long likeCount;

    private Long replyCount;

    private Boolean isLike;

    private LocalDateTime createTime;
}
