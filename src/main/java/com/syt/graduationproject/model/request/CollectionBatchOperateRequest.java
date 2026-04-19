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
public class CollectionBatchOperateRequest {

    /**
     * 来源收藏夹ID
     */
    private Long sourceDirectoryId;

    /**
     * 目标收藏夹ID（取消收藏时可为空）
     */
    private Long targetDirectoryId;

    /**
     * 选中的视频ID列表
     */
    private List<Long> videoIds;

    /**
     * 1: 取消收藏, 2: 复制到目标收藏夹, 3: 移动到目标收藏夹
     */
    private Integer operation;
}

