package com.syt.graduationproject.model.vo;

import com.syt.graduationproject.model.bo.VideoSourceBo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 视频播放页详情
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoPlayDetailVo {

    private Long videoId;

    private Long userId;

    private String title;

    private String description;

    private String coverUrl;

    private List<VideoSourceBo> videoSourceList;

    private String partitionName;

    private Integer duration;

    private Integer lastPlayTime;

    private String username;

    private String avatarUrl;

    private String userBio;

    private Long fansCount;

    private Boolean isFollow;

    private Long playCount;

    private Long likeCount;

    private Long coinCount;

    private Long collectCount;

    private Long shareCount;

    private Long commentCount;

    private Boolean isLike;

    private Boolean isCoin;

    private Boolean isCollect;

    private LocalDateTime createTime;
}
