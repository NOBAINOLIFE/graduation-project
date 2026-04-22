package com.syt.graduationproject.service;

import com.syt.graduationproject.model.request.VideoPlayProgressRequest;
import com.syt.graduationproject.model.request.VideoSubmitRequest;
import com.syt.graduationproject.model.vo.VideoPlayDetailVo;

public interface VideoService {

    /**
     * 查询用户视频数
     */
    Long queryVideoNum(Long userId);

    /**
     * 查询播放页视频详情
     */
    VideoPlayDetailVo queryVideoInfo(Long videoId);

    /**
     * 投稿视频
     */
    void submitVideo(VideoSubmitRequest request);

    /**
     * 发布视频
     */
    void publishVideo(Long videoId);

    /**
     * 上报视频播放进度
     */
    void reportPlayProgress(VideoPlayProgressRequest request);
}
