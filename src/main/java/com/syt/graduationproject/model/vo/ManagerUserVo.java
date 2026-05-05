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
public class ManagerUserVo {

    private Long userId;

    private Long account;

    private String username;

    private String avatarUrl;

    private String bio;

    private Integer status;

    private String statusText;

    private Long videoNum;

    private Long fansNum;

    private Long followNum;

    private Long likeNum;

    private Long playNum;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
