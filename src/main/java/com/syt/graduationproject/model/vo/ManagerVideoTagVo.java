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
public class ManagerVideoTagVo {

    private Long tagId;

    private String tagName;

    private Long hot;

    private Long relatedVideoCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
