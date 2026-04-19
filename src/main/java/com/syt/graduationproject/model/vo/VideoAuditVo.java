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
public class VideoAuditVo {

    private Long videoId;

    private Long userId;

    private String title;

    private String coverUrl;

    private Integer duration;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

