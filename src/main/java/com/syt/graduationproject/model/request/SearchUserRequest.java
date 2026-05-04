package com.syt.graduationproject.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchUserRequest {

    /**
     * 用户名关键词
     */
    private String keyword;

    /**
     * 限定用户ID集合
     */
    private List<Long> userIdList;

    /**
     * 排序方式：1-粉丝数从高到低
     */
    private Integer sortType;

    /**
     * 页码（从1开始）
     */
    private Integer pageNum;

    /**
     * 每页大小
     */
    private Integer pageSize;
}

