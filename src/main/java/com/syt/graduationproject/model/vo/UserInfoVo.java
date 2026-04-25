package com.syt.graduationproject.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoVo {

    private Long userId;

    private String username;

    private String avatarUrl;

    private String bio;

    private Long videoNum;

    private Long fansNum;

    private Long followNum;

    private Long likeNum;

    private Long playNum;

    private Boolean isFollow;

    private Boolean isFans;

    private Boolean isBanned;

    private Boolean isBlack;
}
