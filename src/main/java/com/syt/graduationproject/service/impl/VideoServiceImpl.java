package com.syt.graduationproject.service.impl;

import com.syt.graduationproject.enums.VideoResolutionEnum;
import com.syt.graduationproject.enums.VideoStatusEnum;
import com.syt.graduationproject.enums.VideoTranscodingStatusEnum;
import com.syt.graduationproject.exception.CustomException;
import com.syt.graduationproject.mapper.UserStatsMapper;
import com.syt.graduationproject.mapper.VideoMapper;
import com.syt.graduationproject.mapper.VideoSourceMapper;
import com.syt.graduationproject.mapper.VideoStatsMapper;
import com.syt.graduationproject.model.bo.UserVideoInteractionBo;
import com.syt.graduationproject.model.bo.VideoSourceBo;
import com.syt.graduationproject.model.dto.VideoTranscodingDto;
import com.syt.graduationproject.model.po.*;
import com.syt.graduationproject.model.request.VideoUploadRequest;
import com.syt.graduationproject.model.vo.VideoPlayDetailVo;
import com.syt.graduationproject.model.vo.VideoUploadConfirmVo;
import com.syt.graduationproject.model.vo.VideoUploadVo;
import com.syt.graduationproject.repository.UserRepository;
import com.syt.graduationproject.repository.VideoRepository;
import com.syt.graduationproject.service.InteractService;
import com.syt.graduationproject.service.minio.MinioService;
import com.syt.graduationproject.service.VideoService;
import com.syt.graduationproject.util.UserHolderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.syt.graduationproject.config.KafkaConfig.VIDEO_PROCESS_TOPIC;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final InteractService interactService;

    private final MinioService minioService;

    private final VideoRepository videoRepository;

    private final UserRepository userRepository;

    private final VideoMapper videoMapper;

    private final VideoStatsMapper videoStatsMapper;

    private final UserStatsMapper userStatsMapper;

    private final VideoSourceMapper videoSourceMapper;

    private final KafkaTemplate<String, VideoTranscodingDto> kafkaTemplate;

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
        videoPlayDetailVo.setVideoSourceList(videoSourceList);

        // 查询用户播放记录
        UserVideoHistoryPo userVideoHistoryPo = videoRepository.queryUserVideoHistory(myId, videoId);
        videoPlayDetailVo.setLastPlayTime(userVideoHistoryPo.getLastViewTime());

        // 查询视频数据统计信息
        VideoStatsPo videoStatsPo = videoRepository.queryVideoStatsById(videoId);
        videoPlayDetailVo.setPlayCount(videoStatsPo.getPlayCount());
        videoPlayDetailVo.setLikeCount(videoStatsPo.getLikeCount());
        videoPlayDetailVo.setCoinCount(videoStatsPo.getCoinCount());
        videoPlayDetailVo.setCollectCount(videoStatsPo.getCollectCount());
        videoPlayDetailVo.setShareCount(videoStatsPo.getShareCount());

        // 查询用户与视频的交互情况
        UserVideoInteractionBo userVideoInteractionBo = interactService.queryUserVideoInteraction(myId, videoId);
        videoPlayDetailVo.setIsLike(userVideoInteractionBo.getIsLike());
        videoPlayDetailVo.setIsCoin(userVideoInteractionBo.getIsCoin());
        videoPlayDetailVo.setIsCollect(userVideoInteractionBo.getIsCollect());

        return videoPlayDetailVo;
    }

    /**
     * 获取视频上传链接
     */
    @Override
    public VideoUploadVo askForUpload(VideoUploadRequest request) {
        Long myId = UserHolderUtil.getUser().getUserId();
        // 1. 准备路径（建议按日期分文件夹）
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd/"));
        String title = request.getTitle();
        String videoName = request.getVideoName();
        String coverName = request.getCoverName();
        String videoSuffix = videoName.substring(videoName.lastIndexOf("."));
        String coverSuffix = coverName.substring(coverName.lastIndexOf("."));

        String videoObject = "videos/" + datePath + UUID.randomUUID() + videoSuffix;
        String coverObject = "covers/" + datePath + UUID.randomUUID() + coverSuffix;

        // 2. 预创数据库记录
        VideoPo video = VideoPo.builder()
                .userId(myId)
                .title(title)
                .description(request.getDescription())
                .coverUrl(coverObject)
                .status(VideoStatusEnum.WAITING_UPLOAD.getCode())
                .build();
        videoMapper.insert(video);

        VideoSourcePo videoSource = VideoSourcePo.builder()
                .videoId(video.getId())
                .resolution(VideoResolutionEnum.ORIGINAL.getCode())
                .playUrl(videoObject)
                .status(VideoTranscodingStatusEnum.WAITING_TRANSCODING.getCode())
                .build();
        videoSourceMapper.insert(videoSource);


        // 3. 生成视频上传 URL
        String videoUploadUrl = minioService.generateUrl(videoObject);
        // 4. 生成封面上传 URL
        String coverUploadUrl = minioService.generateUrl(coverObject);

        return VideoUploadVo.builder()
                .videoId(video.getId())
                .videoUrl(videoUploadUrl)
                .coverUrl(coverUploadUrl)
                .build();
    }

    /**
     * 确认视频上传成功
     */
    @Override
    public VideoUploadConfirmVo confirmUpload(Long videoId) {
        VideoPo videoPo = videoMapper.selectById(videoId);
        if (Objects.isNull(videoPo)) {
            throw new CustomException("视频不存在");
        }
        List<VideoSourceBo> videoSourceBoList = videoRepository
                .queryVideoSource(videoId, VideoResolutionEnum.ORIGINAL.getCode());
        if (CollectionUtils.isEmpty(videoSourceBoList)) {
            throw new CustomException("视频源不存在");
        }
        if (!Objects.equals(videoPo.getStatus(), VideoStatusEnum.WAITING_UPLOAD.getCode())) {
            throw new CustomException("视频状态不正确");
        }

        // 获取上传的视频和封面 URL
        String originalResolutionUrl = videoSourceBoList.get(0).getPlayUrl();
        boolean isVideoUploadSuccess = minioService.checkFileExists(originalResolutionUrl);
        boolean isCoverUploadSuccess = minioService.checkFileExists(videoPo.getCoverUrl());
        if (!isVideoUploadSuccess || !isCoverUploadSuccess) {
            return VideoUploadConfirmVo.builder()
                    .isVideoUploadSuccess(isVideoUploadSuccess)
                    .isCoverUploadSuccess(isCoverUploadSuccess)
                    .build();
        }

        // 更新视频状态
        videoPo.setStatus(VideoStatusEnum.UPLOADED.getCode());
        videoMapper.updateById(videoPo);

        // 创建视频统计信息
        VideoStatsPo videoStatsPo = VideoStatsPo.builder()
                .videoId(videoId)
                .build();
        videoStatsMapper.insert(videoStatsPo);

        // 更新用户视频数量
        Long myId = UserHolderUtil.getUser().getUserId();
        UserStatsPo userStatsPo = userRepository.queryUserStatsById(myId);
        if (Objects.nonNull(userStatsPo)) {
            userStatsMapper.updateUserVideoNum(myId, 1L);
        } else {
            userStatsPo = UserStatsPo.builder()
                    .userId(myId)
                    .videoNum(1L)
                    .build();
            userStatsMapper.insert(userStatsPo);
        }

        // 发送 Kafka 消息进行转码
        VideoTranscodingDto videoTranscodingDto = VideoTranscodingDto.builder()
                .videoId(videoId)
                .originalResolutionUrl(originalResolutionUrl)
                .build();
        kafkaTemplate.send(VIDEO_PROCESS_TOPIC, String.valueOf(videoId), videoTranscodingDto);

        log.info("视频上传确认成功，videoId: {}, userId: {}", videoId, myId);
        return VideoUploadConfirmVo.builder()
                .isVideoUploadSuccess(true)
                .isCoverUploadSuccess(true)
                .build();
    }
}
