package com.syt.graduationproject.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSimpleInfoVo {

    private Long userId;

    private String username;

    private String avatarUrl;

    private String bio;

    private Boolean isFollow;
}
