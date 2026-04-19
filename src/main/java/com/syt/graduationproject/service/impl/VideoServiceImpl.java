package com.syt.graduationproject.service.impl;

import com.syt.graduationproject.config.KafkaConfig;
import com.syt.graduationproject.enums.VideoStatusEnum;
import com.syt.graduationproject.exception.CustomException;
import com.syt.graduationproject.model.bo.UserVideoInteractionBo;
import com.syt.graduationproject.model.bo.VideoSourceBo;
import com.syt.graduationproject.model.kafka.VideoProcessMessage;
import com.syt.graduationproject.model.po.*;
import com.syt.graduationproject.model.request.VideoSubmitRequest;
import com.syt.graduationproject.model.vo.VideoPlayDetailVo;
import com.syt.graduationproject.repository.SearchRepository;
import com.syt.graduationproject.repository.UserRepository;
import com.syt.graduationproject.repository.VideoRepository;
import com.syt.graduationproject.service.InteractService;
import com.syt.graduationproject.service.ManagerService;
import com.syt.graduationproject.service.minio.MinioService;
import com.syt.graduationproject.service.VideoService;
import com.syt.graduationproject.util.UserHolderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final InteractService interactService;

    private final MinioService minioService;

    private final VideoRepository videoRepository;

    private final UserRepository userRepository;

    private final SearchRepository searchRepository;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final ManagerService managerService;

    /**
     * 查询用户视频数
     */
    @Override
    public Long queryVideoNum(Long userId) {
        return videoRepository.queryUserVideoNum(userId);
    }

    /**
     * 播放页视频详情
     */
    @Override
    public VideoPlayDetailVo queryVideoInfo(Long videoId) {
        Long myId = UserHolderUtil.getUser().getUserId();
        VideoPo videoPo = videoRepository.queryVideoById(videoId);
        if (Objects.isNull(videoPo)) {
            throw new CustomException("视频不存在");
        }
        if (VideoStatusEnum.PUBLISHED.getCode() != videoPo.getStatus()) {
            throw new CustomException("视频暂未发布");
        }
        // 查询视频元信息
        VideoPlayDetailVo videoPlayDetailVo = VideoPlayDetailVo.builder()
                .videoId(videoPo.getId())
                .userId(videoPo.getUserId())
                .title(videoPo.getTitle())
                .description(videoPo.getDescription())
                .coverUrl(videoPo.getCoverUrl())
                .duration(videoPo.getDuration())
                .createTime(videoPo.getCreateTime())
                .build();

        // 查询视频播放源
        List<VideoSourceBo> videoSourceList = videoRepository.queryVideoSource(videoId, null);
        for (VideoSourceBo sourceBo : videoSourceList) {
            if (StringUtils.isNotBlank(sourceBo.getPlayUrl())) {
                sourceBo.setPlayUrl(minioService.generateGetUrl(sourceBo.getPlayUrl(), 30));
            }
        }
        videoPlayDetailVo.setVideoSourceList(videoSourceList);

        // 查询用户播放记录
        UserVideoHistoryPo userVideoHistoryPo = videoRepository.queryUserVideoHistory(myId, videoId);
        if (Objects.nonNull(userVideoHistoryPo)) {
            videoPlayDetailVo.setLastPlayTime(userVideoHistoryPo.getLastViewTime());
        }

        // 查询视频数据统计信息
        VideoStatsPo videoStatsPo = videoRepository.queryVideoStatsById(videoId);
        if (videoStatsPo != null) {
            videoPlayDetailVo.setPlayCount(videoStatsPo.getPlayCount());
            videoPlayDetailVo.setLikeCount(videoStatsPo.getLikeCount());
            videoPlayDetailVo.setCoinCount(videoStatsPo.getCoinCount());
            videoPlayDetailVo.setCollectCount(videoStatsPo.getCollectCount());
            videoPlayDetailVo.setShareCount(videoStatsPo.getShareCount());
        }

        // 查询用户与视频的交互情况
        UserVideoInteractionBo userVideoInteractionBo = interactService.queryUserVideoInteraction(myId, videoId);
        videoPlayDetailVo.setIsLike(userVideoInteractionBo.getIsLike());
        videoPlayDetailVo.setIsCoin(userVideoInteractionBo.getIsCoin());
        videoPlayDetailVo.setIsCollect(userVideoInteractionBo.getIsCollect());

        return videoPlayDetailVo;
    }

    @Override
    public void submitVideo(VideoSubmitRequest request) {
        if (request == null || request.getVideoId() == null || StringUtils.isBlank(request.getTitle())
                || StringUtils.isBlank(request.getCoverUrl()) || request.getDuration() == null || request.getDuration() <= 0) {
            throw new CustomException("投稿参数不完整");
        }
        Long userId = UserHolderUtil.getUser().getUserId();
        VideoPo videoPo = videoRepository.queryVideoByIdAndUserId(request.getVideoId(), userId);
        if (videoPo == null) {
            throw new CustomException("视频不存在或无权限投稿");
        }

        int updated = videoRepository.submitVideo(
                request.getVideoId(),
                userId,
                request.getTitle(),
                request.getDescription(),
                request.getCoverUrl(),
                request.getDuration()
        );
        if (updated <= 0) {
            throw new CustomException("当前视频状态不允许投稿");
        }

        videoRepository.insertVideoStatsIfAbsent(request.getVideoId());
        kafkaTemplate.send(KafkaConfig.VIDEO_PROCESS_TOPIC,
                VideoProcessMessage.builder().videoId(request.getVideoId()).userId(userId).build());
    }

    @Override
    public void publishVideo(Long videoId) {
        Long userId = UserHolderUtil.getUser().getUserId();
        VideoPo videoPo = videoRepository.queryVideoByIdAndUserId(videoId, userId);
        if (videoPo == null) {
            throw new CustomException("视频不存在或无权限发布");
        }
        if (VideoStatusEnum.TRANSCODE_SUCCESS.getCode() == videoPo.getStatus()
                || VideoStatusEnum.AUDIT_REJECTED.getCode() == videoPo.getStatus()) {
            int updated = videoRepository.updateVideoStatus(videoId, videoPo.getStatus(), VideoStatusEnum.AUDITING.getCode());
            if (updated <= 0) {
                throw new CustomException("提交审核失败，请稍后重试");
            }
            managerService.createAuditingRecord(videoId, userId);
            return;
        }
        if (VideoStatusEnum.AUDITING.getCode() == videoPo.getStatus()) {
            return;
        }
        throw new CustomException("当前视频状态不允许提交审核");
    }
}
