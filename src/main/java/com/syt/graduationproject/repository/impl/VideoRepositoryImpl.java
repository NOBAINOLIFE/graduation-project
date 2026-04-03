package com.syt.graduationproject.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.syt.graduationproject.enums.VideoStatusEnum;
import com.syt.graduationproject.mapper.UserVideoHistoryMapper;
import com.syt.graduationproject.mapper.VideoMapper;
import com.syt.graduationproject.mapper.VideoStatsMapper;
import com.syt.graduationproject.model.po.UserVideoHistoryPo;
import com.syt.graduationproject.model.po.VideoPo;
import com.syt.graduationproject.model.po.VideoStatsPo;
import com.syt.graduationproject.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import static com.syt.graduationproject.constant.CommonConstant.NOT_DELETED;

@Repository
@RequiredArgsConstructor
public class VideoRepositoryImpl implements VideoRepository {

    private final VideoMapper videoMapper;

    private final VideoStatsMapper videoStatsMapper;

    private final UserVideoHistoryMapper userVideoHistoryMapper;

    /**
     * 查询用户视频数
     */
    @Override
    public Long queryUserVideoNum(Long userId) {
        QueryWrapper<VideoPo> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(VideoPo::getUserId, userId)
                .eq(VideoPo::getStatus, VideoStatusEnum.NORMAL.getCode());
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
}
