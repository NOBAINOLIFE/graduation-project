package com.syt.graduationproject.model.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchVideoRequest {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 视频标题关键词
     */
    private String keyword;

    /**
     * 限定视频ID集合
     */
    private List<Long> videoIdList;

    /**
     * 分区ID
     */
    private Long partitionId;

    /**
     * 排序方式：1-最多播放 2-最新发布 3-最多收藏
     */
    private Integer sortType;

    /**
     * 时长下限（秒）
     */
    private Integer minDuration;

    /**
     * 时长上限（秒）
     */
    private Integer maxDuration;

    /**
     * 发布日期开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime publishStartTime;

    /**
     * 发布日期结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime publishEndTime;

    /**
     * 页码（从1开始）
     */
    private Integer pageNum;

    /**
     * 每页大小
     */
    private Integer pageSize;
}

