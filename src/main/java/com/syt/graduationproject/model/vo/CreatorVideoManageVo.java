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
public class CreatorVideoManageVo {

    private Long videoId;

    private String title;

    private String description;

    private String coverUrl;

    private Integer duration;

    private Long partitionId;

    private String partitionName;

    private Integer status;

    private String statusText;

    private Long playCount;

    private Long likeCount;

    private Long collectCount;

    private Long commentCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
