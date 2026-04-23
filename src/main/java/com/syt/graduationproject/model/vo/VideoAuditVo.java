package com.syt.graduationproject.model.vo;

import com.syt.graduationproject.model.bo.VideoSourceBo;
import com.syt.graduationproject.model.po.VideoTagPo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoAuditVo {

    private Long videoId;

    private List<VideoSourceBo> videoSourceList;

    private String title;

    private String coverUrl;

    private String description;

    private String partitionName;

    private List<VideoTagVo> tagList;

    private Long userId;

    private String username;

    private String avatar;

    private Integer status;

    private Long reviewerId;

    private String reviewerName;

    private String reviewerAvatar;

    private String reviewNote;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

