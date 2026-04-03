package com.syt.graduationproject.model.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户视频交互信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVideoInteractionBo {

    private Long userId;

    private Long videoId;

    private Boolean isLike;

    private Boolean isCoin;

    private Boolean isCollect;
}
