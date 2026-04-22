package com.syt.graduationproject.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.syt.graduationproject.config.KafkaConfig;
import com.syt.graduationproject.enums.VideoStatusEnum;
import com.syt.graduationproject.exception.CustomException;
import com.syt.graduationproject.mapper.VideoPartitionMapper;
import com.syt.graduationproject.mapper.VideoTagMapper;
import com.syt.graduationproject.mapper.VideoTagRelMapper;
import com.syt.graduationproject.model.bo.UserVideoInteractionBo;
import com.syt.graduationproject.model.bo.VideoSourceBo;
import com.syt.graduationproject.model.kafka.VideoProcessMessage;
import com.syt.graduationproject.model.po.*;
import com.syt.graduationproject.model.request.VideoPlayProgressRequest;
import com.syt.graduationproject.model.request.VideoSubmitRequest;
import com.syt.graduationproject.model.vo.VideoPlayDetailVo;
import com.syt.graduationproject.model.vo.VideoPartitionVo;
import com.syt.graduationproject.repository.VideoRepository;
import com.syt.graduationproject.service.InteractService;
import com.syt.graduationproject.service.ManagerService;
import com.syt.graduationproject.service.minio.MinioService;
import com.syt.graduationproject.service.VideoService;
import com.syt.graduationproject.util.RedisKeyUtil;
import com.syt.graduationproject.util.UserHolderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private static final int MAX_TAG_COUNT = 10;

    private static final long PV_DEDUP_WINDOW_SECONDS = 30L;

    private static final long PV_DELTA_KEY_TTL_HOURS = 24L;

    private final InteractService interactService;

    private final MinioService minioService;

    private final VideoRepository videoRepository;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final StringRedisTemplate stringRedisTemplate;

    private final ManagerService managerService;

    private final VideoPartitionMapper videoPartitionMapper;

    private final VideoTagMapper videoTagMapper;

    private final VideoTagRelMapper videoTagRelMapper;

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
    @Transactional(rollbackFor = Exception.class)
    public void submitVideo(VideoSubmitRequest request) {
        if (request == null || request.getVideoId() == null || StringUtils.isBlank(request.getTitle())
                || StringUtils.isBlank(request.getCoverUrl()) || request.getDuration() == null || request.getDuration() <= 0
                || request.getPartitionId() == null) {
            throw new CustomException("投稿参数不完整");
        }

        VideoPartitionPo partitionPo = videoPartitionMapper.selectById(request.getPartitionId());
        if (partitionPo == null) {
            throw new CustomException("视频分区不存在");
        }
        List<String> normalizedTagList = normalizeTagList(request.getTagList());

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
                request.getDuration(),
                request.getPartitionId()
        );
        if (updated <= 0) {
            throw new CustomException("当前视频状态不允许投稿");
        }

        bindVideoTags(request.getVideoId(), normalizedTagList);

        videoRepository.insertVideoStatsIfAbsent(request.getVideoId());
        kafkaTemplate.send(KafkaConfig.VIDEO_PROCESS_TOPIC,
                VideoProcessMessage.builder().videoId(request.getVideoId()).userId(userId).build());
    }

    @Override
    public void publishVideo(Long videoId) {
        if (videoId == null) {
            throw new CustomException("发布参数不完整");
        }
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reportPlayProgress(VideoPlayProgressRequest request) {
        if (request == null || request.getVideoId() == null || request.getLastPlayTime() == null) {
            throw new CustomException("播放进度参数不完整");
        }
        Long userId = UserHolderUtil.getUser().getUserId();
        VideoPo videoPo = videoRepository.queryVideoById(request.getVideoId());
        if (videoPo == null) {
            throw new CustomException("视频不存在");
        }
        if (VideoStatusEnum.PUBLISHED.getCode() != videoPo.getStatus()) {
            throw new CustomException("视频暂不可播放");
        }

        int duration = videoPo.getDuration() == null ? 0 : Math.max(videoPo.getDuration(), 0);
        int lastPlayTime = normalizeLastPlayTime(request.getLastPlayTime(), duration);
        int isFinished = duration > 0 && lastPlayTime >= duration ? 1 : 0;

        videoRepository.upsertUserVideoHistory(userId, request.getVideoId(), lastPlayTime, duration, isFinished);

        if (shouldIncreasePv(request.getVideoId(), userId)) {
            String pvDeltaKey = RedisKeyUtil.videoPlayPvDeltaKey();
            stringRedisTemplate.opsForHash().increment(pvDeltaKey, String.valueOf(request.getVideoId()), 1L);
            stringRedisTemplate.expire(pvDeltaKey, PV_DELTA_KEY_TTL_HOURS, TimeUnit.HOURS);
        }
    }

    private int normalizeLastPlayTime(Integer rawLastPlayTime, int duration) {
        int safeLastPlayTime = rawLastPlayTime == null ? 0 : Math.max(rawLastPlayTime, 0);
        if (duration > 0) {
            return Math.min(safeLastPlayTime, duration);
        }
        return safeLastPlayTime;
    }

    private boolean shouldIncreasePv(Long videoId, Long userId) {
        String dedupKey = RedisKeyUtil.videoPlayPvDedupKey(videoId, userId);
        Boolean firstReport = stringRedisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", PV_DEDUP_WINDOW_SECONDS, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(firstReport);
    }

    private void bindVideoTags(Long videoId, List<String> normalizedTagList) {
        List<VideoTagPo> targetTagPos = loadOrCreateTags(normalizedTagList);
        Set<Long> targetTagIdSet = targetTagPos.stream().map(VideoTagPo::getId).collect(Collectors.toSet());

        LambdaQueryWrapper<VideoTagRelPo> relQueryWrapper = new LambdaQueryWrapper<>();
        relQueryWrapper.eq(VideoTagRelPo::getVideoId, videoId);
        List<VideoTagRelPo> existedRelList = videoTagRelMapper.selectList(relQueryWrapper);
        Set<Long> existedTagIdSet = existedRelList.stream().map(VideoTagRelPo::getTagId).collect(Collectors.toSet());

        videoTagRelMapper.delete(relQueryWrapper);
        LocalDateTime now = LocalDateTime.now();
        for (Long tagId : targetTagIdSet) {
            videoTagRelMapper.insert(VideoTagRelPo.builder()
                    .videoId(videoId)
                    .tagId(tagId)
                    .createTime(now)
                    .updateTime(now)
                    .build());
        }

        List<Long> hotIncrementTagIds = targetTagIdSet.stream()
                .filter(tagId -> !existedTagIdSet.contains(tagId))
                .collect(Collectors.toList());
        if (!hotIncrementTagIds.isEmpty()) {
            videoTagMapper.increaseHotBatch(hotIncrementTagIds);
        }
    }

    private List<String> normalizeTagList(List<String> tagList) {
        if (tagList == null || tagList.isEmpty()) {
            throw new CustomException("视频标签不能为空");
        }
        List<String> normalizedTagList = tagList.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (normalizedTagList.isEmpty()) {
            throw new CustomException("视频标签不能为空");
        }
        if (normalizedTagList.size() > MAX_TAG_COUNT) {
            throw new CustomException("视频标签最多可填写10个");
        }
        return normalizedTagList;
    }

    private List<VideoTagPo> loadOrCreateTags(List<String> tagNameList) {
        LambdaQueryWrapper<VideoTagPo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(VideoTagPo::getTagName, tagNameList);
        List<VideoTagPo> existedTagList = videoTagMapper.selectList(queryWrapper);
        Map<String, VideoTagPo> existedTagMap = existedTagList.stream()
                .collect(Collectors.toMap(VideoTagPo::getTagName, item -> item));

        List<String> missingTagList = new ArrayList<>();
        for (String tagName : tagNameList) {
            if (!existedTagMap.containsKey(tagName)) {
                missingTagList.add(tagName);
            }
        }
        if (!missingTagList.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            for (String tagName : missingTagList) {
                try {
                    videoTagMapper.insert(VideoTagPo.builder()
                            .tagName(tagName)
                            .hot(0L)
                            .createTime(now)
                            .updateTime(now)
                            .build());
                } catch (DuplicateKeyException ignore) {
                    // Concurrent creation is handled by the unique index and a re-query below.
                }
            }
            LambdaQueryWrapper<VideoTagPo> reQueryWrapper = new LambdaQueryWrapper<>();
            reQueryWrapper.in(VideoTagPo::getTagName, tagNameList);
            existedTagList = videoTagMapper.selectList(reQueryWrapper);
        }
        if (existedTagList == null || existedTagList.size() != tagNameList.size()) {
            throw new CustomException("创建视频标签失败，请稍后重试");
        }
        return existedTagList;
    }

    @Override
    public List<VideoPartitionVo> listAllPartitions() {
        List<VideoPartitionPo> partitionPoList = videoPartitionMapper.selectList(null);
        return partitionPoList.stream()
                .map(po -> VideoPartitionVo.builder()
                        .id(po.getId())
                        .partitionName(po.getPartitionName())
                        .build())
                .collect(Collectors.toList());
    }
}
