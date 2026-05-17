package com.syt.graduationproject.service;

import com.syt.graduationproject.model.request.CreatorVideoQueryRequest;
import com.syt.graduationproject.model.request.VideoPlayProgressRequest;
import com.syt.graduationproject.model.request.VideoSubmitRequest;
import com.syt.graduationproject.model.request.VideoUpdateRequest;
import com.syt.graduationproject.model.vo.*;
import com.syt.graduationproject.model.vo.Page.PageVo;

import java.util.List;

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
     * 更新视频元信息
     */
    void updateVideo(Long videoId, VideoUpdateRequest request);

    /**
     * 发布视频
     */
    void publishVideo(Long videoId);

    /**
     * 上报视频播放进度
     */
    void reportPlayProgress(VideoPlayProgressRequest request);

    /**
     * 查询所有视频分区
     */
    List<VideoPartitionVo> listAllPartitions();

    List<SearchVideoVo> getVideoPlayList(Long lastVideoId);

    List<UserVideoHistoryVo> listUserVideoHistory(Integer pageNum, Integer pageSize);

    PageVo<CreatorVideoManageVo> listCreatorVideos(CreatorVideoQueryRequest request);
}
