package com.syt.graduationproject.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syt.graduationproject.model.po.*;

import java.util.List;

public interface VideoRepository {

    /**
     * 创建上传中的视频记录
     */
    Long createUploadingVideo(Long userId);

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
     * 查询用户某个视频的播放记录
     */
    UserVideoHistoryPo queryUserVideoHistory(Long userId, Long videoId);

    /**
     * 批量查询用户视频播放记录
     */
    List<UserVideoHistoryPo> batchQueryUserVideoHistory(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 查询视频播放源
     */
    List<VideoSourcePo> queryVideoSource(Long videoId, Integer resolution, boolean withoutOriginal);

    /**
     * 根据视频ID和作者ID查询视频
     */
    VideoPo queryVideoByIdAndUserId(Long videoId, Long userId);

    /**
     * 更新视频状态（可选CAS）
     */
    int updateVideoStatus(Long videoId, Integer expectedStatus, Integer targetStatus);

    /**
     * 更新投稿信息并进入待转码
     */
    int submitVideo(Long videoId, Long userId, String title, String description, String coverUrl, Integer duration, Long partitionId);

    /**
     * 更新视频元信息（标题/封面/分区/简介）
     */
    int updateVideo(Long videoId, Long userId, String title, String description, String coverUrl, Long partitionId);

    /**
     * 插入视频统计记录
     */
    int insertVideoStatsIfAbsent(Long videoId);

    /**
     * 保存视频源记录
     */
    int saveVideoSource(VideoSourcePo videoSourcePo);

    /**
     * 删除视频源记录
     */
    int deleteVideoSource(Long videoId, Integer resolution);

    /**
     * 保存或更新用户观看进度
     */
    int upsertUserVideoHistory(Long userId, Long videoId, Integer lastPlayTime, Integer duration, Integer isFinished);

    /**
     * 增加视频播放量
     */
    int incrVideoPlayCount(Long videoId, Long delta);

    /**
     * 查询视频标签
     */
    List<VideoTagPo> queryVideoTags(Long videoId);

    /**
     * 查询创作者稿件列表
     */
    Page<VideoPo> queryCreatorVideoPage(Long userId, String keyword, Integer status, Integer pageNum, Integer pageSize);

    int batchAddVideoCollectCount(List<Long> videoIdList, Long delta);
}
