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
public class UserCoinChangeLogVo {

    private Long id;

    private Integer changeAmount;

    private String changeDesc;

    private Integer changeType;

    private Long relatedTargetId;

    private LocalDateTime createTime;
}
