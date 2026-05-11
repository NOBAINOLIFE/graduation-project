package com.syt.graduationproject.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchUserVo {

    private Long userId;

    private String username;

    private String avatar;

    private Long fansCount;

    private Long videoCount;

    private String bio;

    private Boolean isFollow;

    /**
     * 命中的用户名高亮片段（含 em 标签）
     */
    private String highlightUsername;
}

