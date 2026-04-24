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
public class SearchVideoVo {

    private Long videoId;

    private String title;

    private Long userId;

    private String username;

    private String coverUrl;

    private Long playCount;

    private LocalDateTime createTime;

    private Long collectionCount;

    private Integer duration;

    private LocalDateTime collectTime;
}

