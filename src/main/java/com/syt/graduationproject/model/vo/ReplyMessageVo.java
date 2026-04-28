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
public class ReplyMessageVo {

    private Long replyCommentId;

    private Long replierUserId;

    private String replierUsername;

    private String replierAvatarUrl;

    private String replyContent;

    private Long originalCommentId;

    private String originalCommentContent;

    private Long videoId;

    private String videoTitle;

    private LocalDateTime createTime;
}
