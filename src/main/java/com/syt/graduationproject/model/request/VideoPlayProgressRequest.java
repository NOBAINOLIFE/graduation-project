package com.syt.graduationproject.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 播放进度上报请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoPlayProgressRequest {

    /**
     * 视频ID
     */
    private Long videoId;

    /**
     * 上次播放进度（秒）
     */
    private Integer lastPlayTime;
}

