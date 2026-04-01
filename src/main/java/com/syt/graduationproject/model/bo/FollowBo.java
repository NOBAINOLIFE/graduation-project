package com.syt.graduationproject.model.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowBo {

    /**
     * 是否关注
     */
    private Boolean isFollow;

    /**
     * 是否被关注
     */
    private Boolean isFans;
}
