package com.syt.graduationproject.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syt.graduationproject.converter.VideoConverter;
import com.syt.graduationproject.enums.VideoResolutionEnum;
import com.syt.graduationproject.enums.VideoStatusEnum;
import com.syt.graduationproject.mapper.*;
import com.syt.graduationproject.model.po.*;
import com.syt.graduationproject.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.syt.graduationproject.constant.CommonConstant.NOT_DELETED;

@Repository
@RequiredArgsConstructor
public class VideoRepositoryImpl implements VideoRepository {

    private final VideoMapper videoMapper;

    private final VideoStatsMapper videoStatsMapper;

    private final UserVideoHistoryMapper userVideoHistoryMapper;

    private final VideoSourceMapper videoSourceMapper;

    private final VideoConverter videoConverter;

    private final VideoTagMapper videoTagMapper;

    private final VideoTagRelMapper videoTagRelMapper;

    /**
     * 创建上传中的视频记录
     */
    @Override
    public Long createUploadingVideo(Long userId) {
        VideoPo videoPo = VideoPo.builder()
                .userId(userId)
                .status(VideoStatusEnum.UPLOADING.getCode())
                .build();
        videoMapper.insert(videoPo);
        return videoPo.getId();
    }

    /**
     * 查询用户视频数
     */
    @Override
    public Long queryUserVideoNum(Long userId) {
        QueryWrapper<VideoPo> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(VideoPo::getUserId, userId)
                .eq(VideoPo::getStatus, VideoStatusEnum.PUBLISHED.getCode());
        return videoMapper.selectCount(queryWrapper);
    }

    /**
     * 根据视频ID查询视频元信息
     */
    @Override
    public VideoPo queryVideoById(Long videoId) {
        return videoMapper.selectById(videoId);
    }

    /**
     * 根据视频ID查询视频统计信息
     */
    @Override
    public VideoStatsPo queryVideoStatsById(Long videoId) {
        QueryWrapper<VideoStatsPo> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(VideoStatsPo::getVideoId, videoId)
                .eq(VideoStatsPo::getIsDeleted, NOT_DELETED);
        return videoStatsMapper.selectOne(queryWrapper);
    }

    /**
     * 根据用户ID和视频ID查询用户观看历史
     */
    @Override
    public UserVideoHistoryPo queryUserVideoHistory(Long userId, Long videoId) {
        QueryWrapper<UserVideoHistoryPo> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(UserVideoHistoryPo::getUserId, userId)
                .eq(UserVideoHistoryPo::getVideoId, videoId)
                .eq(UserVideoHistoryPo::getIsDeleted, NOT_DELETED);
        return userVideoHistoryMapper.selectOne(queryWrapper);
    }

    /**
     * 批量查询用户观看历史
     */
    @Override
    public List<UserVideoHistoryPo> batchQueryUserVideoHistory(Long userId, Integer pageNum, Integer pageSize) {
        Page<UserVideoHistoryPo> page = new Page<>(pageNum, pageSize);
        QueryWrapper<UserVideoHistoryPo> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(UserVideoHistoryPo::getUserId, userId)
                .eq(UserVideoHistoryPo::getIsDeleted, NOT_DELETED)
                .orderByDesc(UserVideoHistoryPo::getUpdateTime)
                .orderByDesc(UserVideoHistoryPo::getId);
        return userVideoHistoryMapper.selectPage(page, queryWrapper).getRecords();
    }

    @Override
    public List<VideoSourcePo> queryVideoSource(Long videoId, Integer resolution, boolean withoutOriginal) {
        LambdaQueryWrapper<VideoSourcePo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VideoSourcePo::getVideoId, videoId)
                .eq(VideoSourcePo::getIsDeleted, NOT_DELETED);
        if (resolution != null) {
            queryWrapper.eq(VideoSourcePo::getResolutionCode, resolution);
        }
        if (withoutOriginal) {
            queryWrapper.ne(VideoSourcePo::getResolutionCode, VideoResolutionEnum.ORIGINAL.getCode());
        }
        queryWrapper.orderByDesc(VideoSourcePo::getResolutionCode)
                .orderByAsc(VideoSourcePo::getId);
        return videoSourceMapper.selectList(queryWrapper);
    }

    @Override
    public VideoPo queryVideoByIdAndUserId(Long videoId, Long userId) {
        LambdaQueryWrapper<VideoPo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VideoPo::getId, videoId)
                .eq(VideoPo::getUserId, userId);
        return videoMapper.selectOne(queryWrapper);
    }

    @Override
    public int updateVideoStatus(Long videoId, Integer expectedStatus, Integer targetStatus) {
        LambdaUpdateWrapper<VideoPo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(VideoPo::getId, videoId)
                .set(VideoPo::getStatus, targetStatus)
                .set(VideoPo::getUpdateTime, LocalDateTime.now());
        if (expectedStatus != null) {
            updateWrapper.eq(VideoPo::getStatus, expectedStatus);
        }
        return videoMapper.update(null, updateWrapper);
    }

    @Override
    public int submitVideo(Long videoId, Long userId, String title, String description, String coverUrl, Integer duration, Long partitionId) {
        LambdaUpdateWrapper<VideoPo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(VideoPo::getId, videoId)
                .eq(VideoPo::getUserId, userId)
                .eq(VideoPo::getStatus, VideoStatusEnum.UPLOAD_SUCCESS.getCode())
                .set(VideoPo::getTitle, title)
                .set(VideoPo::getDescription, description)
                .set(VideoPo::getCoverUrl, coverUrl)
                .set(VideoPo::getDuration, duration)
                .set(VideoPo::getPartitionId, partitionId)
                .set(VideoPo::getStatus, VideoStatusEnum.WAITING_TRANSCODE.getCode())
                .set(VideoPo::getUpdateTime, LocalDateTime.now());
        return videoMapper.update(null, updateWrapper);
    }

    @Override
    public int insertVideoStatsIfAbsent(Long videoId) {
        LambdaQueryWrapper<VideoStatsPo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VideoStatsPo::getVideoId, videoId)
                .eq(VideoStatsPo::getIsDeleted, NOT_DELETED);
        VideoStatsPo exist = videoStatsMapper.selectOne(queryWrapper);
        if (exist != null) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        VideoStatsPo videoStatsPo = VideoStatsPo.builder()
                .videoId(videoId)
                .playCount(0L)
                .likeCount(0L)
                .coinCount(0L)
                .collectCount(0L)
                .shareCount(0L)
                .commentCount(0L)
                .isDeleted(NOT_DELETED)
                .createTime(now)
                .updateTime(now)
                .build();
        return videoStatsMapper.insert(videoStatsPo);
    }

    @Override
    public int saveVideoSource(VideoSourcePo videoSourcePo) {
        return videoSourceMapper.insert(videoSourcePo);
    }

    @Override
    public int deleteVideoSource(Long videoId, Integer resolution) {
        LambdaQueryWrapper<VideoSourcePo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VideoSourcePo::getVideoId, videoId)
                .eq(VideoSourcePo::getResolutionCode, resolution);
        return videoSourceMapper.delete(queryWrapper);
    }

    @Override
    public int upsertUserVideoHistory(Long userId, Long videoId, Integer lastPlayTime, Integer duration, Integer isFinished) {
        return userVideoHistoryMapper.upsertHistory(userId, videoId, lastPlayTime, duration, isFinished);
    }

    @Override
    public int incrVideoPlayCount(Long videoId, Long delta) {
        return videoStatsMapper.updatePlayCount(videoId, delta);
    }

    @Override
    public List<VideoTagPo> queryVideoTags(Long videoId) {
        LambdaQueryWrapper<VideoTagRelPo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VideoTagRelPo::getVideoId, videoId);
        List<VideoTagRelPo> relList = videoTagRelMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(relList)) {
            return Collections.emptyList();
        }
        List<Long> tagIdList = relList.stream()
                .map(VideoTagRelPo::getTagId)
                .collect(Collectors.toList());
        return videoTagMapper.selectBatchIds(tagIdList);
    }

    /**
     * 批量增加视频收藏数
     */
    @Override
    public int batchAddVideoCollectCount(List<Long> videoIdList, Long delta) {
        return videoStatsMapper.batchAddVideoCollectCount(videoIdList, delta);
    }
}
