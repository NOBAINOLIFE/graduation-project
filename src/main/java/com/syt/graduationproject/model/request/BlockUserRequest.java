package com.syt.graduationproject.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockUserRequest {

    /**
     * 被操作用户ID
     */
    private Long targetUserId;

    /**
     * 1: 拉黑, 0: 取消拉黑
     */
    private Integer operation;
}

