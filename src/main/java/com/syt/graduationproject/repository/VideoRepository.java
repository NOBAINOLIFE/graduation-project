package com.syt.graduationproject.repository;

import com.syt.graduationproject.model.bo.VideoSourceBo;
import com.syt.graduationproject.model.po.UserVideoHistoryPo;
import com.syt.graduationproject.model.po.VideoPo;
import com.syt.graduationproject.model.po.VideoStatsPo;

import java.util.List;

public interface VideoRepository {

    /**
     * 查询用户视频数
     */
    Long queryUserVideoNum(Long userId);

    /**
     * 根据ID查询视频元信息
     */
    VideoPo queryVideoById(Long videoId);

    /**
     * 根据ID查询视频数据统计信息
     */
    VideoStatsPo queryVideoStatsById(Long videoId);

    /**
     * 查询用户视频播放记录
     */
    UserVideoHistoryPo queryUserVideoHistory(Long userId, Long videoId);

    /**
     * 查询视频播放源
     */
    List<VideoSourceBo> queryVideoSource(Long videoId, Integer resolution);
}
