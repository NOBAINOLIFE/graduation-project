package com.syt.graduationproject.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeReceivedDetailVo {

    /**
     * comment / video
     */
    private String targetType;

    private Long targetId;

    private Long commentId;

    private String commentContent;

    private Long videoId;

    private String videoTitle;

    private String videoCoverUrl;

    private Long totalCount;

    private List<LikeUserVo> recentUsers;
}
