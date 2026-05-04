package com.syt.graduationproject.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syt.graduationproject.config.KafkaConfig;
import com.syt.graduationproject.converter.VideoConverter;
import com.syt.graduationproject.enums.VideoStatusEnum;
import com.syt.graduationproject.exception.CustomException;
import com.syt.graduationproject.mapper.VideoPartitionMapper;
import com.syt.graduationproject.mapper.VideoStatsMapper;
import com.syt.graduationproject.mapper.VideoTagMapper;
import com.syt.graduationproject.mapper.VideoTagRelMapper;
import com.syt.graduationproject.model.bo.FollowBo;
import com.syt.graduationproject.model.bo.UserVideoInteractionBo;
import com.syt.graduationproject.model.vo.VideoSourceVo;
import com.syt.graduationproject.model.kafka.VideoProcessMessage;
import com.syt.graduationproject.model.po.*;
import com.syt.graduationproject.model.request.CreatorVideoQueryRequest;
import com.syt.graduationproject.model.request.VideoPlayProgressRequest;
import com.syt.graduationproject.model.request.VideoSubmitRequest;
import com.syt.graduationproject.model.vo.CreatorVideoManageVo;
import com.syt.graduationproject.model.vo.Page.PageVo;
import com.syt.graduationproject.model.vo.SearchVideoVo;
import com.syt.graduationproject.model.vo.UserVideoHistoryVo;
import com.syt.graduationproject.repository.SearchRepository;
import com.syt.graduationproject.converter.SearchConverter;
import com.syt.graduationproject.model.es.VideoEsDoc;

import static com.syt.graduationproject.constant.VideoConstant.HOME_PAGE_VIDEO_LIST_SIZE;
import com.syt.graduationproject.model.vo.VideoPlayDetailVo;
import com.syt.graduationproject.model.vo.VideoPartitionVo;
import com.syt.graduationproject.repository.UserRepository;
import com.syt.graduationproject.repository.VideoRepository;
import com.syt.graduationproject.service.InteractRelationService;
import com.syt.graduationproject.service.InteractService;
import com.syt.graduationproject.service.ManagerService;
import com.syt.graduationproject.service.minio.MinioService;
import com.syt.graduationproject.service.VideoService;
import com.syt.graduationproject.util.RedisKeyUtil;
import com.syt.graduationproject.util.UserHolderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private static final int MAX_TAG_COUNT = 10;

    private static final long PV_DEDUP_WINDOW_SECONDS = 30L;

    private static final long PV_DELTA_KEY_TTL_HOURS = 24L;

    private final UserRepository userRepository;

    private final MinioService minioService;

    private final VideoRepository videoRepository;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final StringRedisTemplate stringRedisTemplate;

    private final ManagerService managerService;

    private final VideoPartitionMapper videoPartitionMapper;

    private final VideoStatsMapper videoStatsMapper;

    private final VideoTagMapper videoTagMapper;

    private final VideoTagRelMapper videoTagRelMapper;

    private final SearchRepository searchRepository;

    private final SearchConverter searchConverter;

    private final VideoConverter videoConverter;

    private final InteractRelationService interactRelationService;

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
        Long myId = getCurrentUserIdOrNull();
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
                .partitionName(loadPartitionName(videoPo.getPartitionId()))
                .duration(videoPo.getDuration())
                .createTime(videoPo.getCreateTime())
                .build();

        // 查询视频播放源
        List<VideoSourcePo> videoSourceList = videoRepository.queryVideoSource(videoId, null, true);
        for (VideoSourcePo sourcePo : videoSourceList) {
            if (StringUtils.isNotBlank(sourcePo.getPlayUrl())) {
                sourcePo.setPlayUrl(minioService.generateGetUrl(sourcePo.getPlayUrl(), 30));
            }
        }
        videoPlayDetailVo.setVideoSourceList(videoConverter.toVideoSourceVoList(videoSourceList));

        // 查询用户播放记录
        if (myId != null) {
            UserVideoHistoryPo userVideoHistoryPo = videoRepository.queryUserVideoHistory(myId, videoId);
            if (Objects.nonNull(userVideoHistoryPo)) {
                videoPlayDetailVo.setLastPlayTime(userVideoHistoryPo.getLastPlayTime());
            }
        }

        // 查询视频数据统计信息
        VideoStatsPo videoStatsPo = videoRepository.queryVideoStatsById(videoId);
        if (videoStatsPo != null) {
            videoPlayDetailVo.setPlayCount(videoStatsPo.getPlayCount());
            videoPlayDetailVo.setLikeCount(videoStatsPo.getLikeCount());
            videoPlayDetailVo.setCoinCount(videoStatsPo.getCoinCount());
            videoPlayDetailVo.setCollectCount(videoStatsPo.getCollectCount());
            videoPlayDetailVo.setShareCount(videoStatsPo.getShareCount());
            videoPlayDetailVo.setCommentCount(videoStatsPo.getCommentCount());
        }

        // 查询用户与视频的交互情况
        if (myId != null) {
            UserVideoInteractionBo userVideoInteractionBo = interactRelationService.queryUserVideoInteraction(myId, videoId);
            videoPlayDetailVo.setIsLike(userVideoInteractionBo.getIsLike());
            videoPlayDetailVo.setIsCoin(userVideoInteractionBo.getCoinCount() != 0);
            videoPlayDetailVo.setIsCollect(userVideoInteractionBo.getIsCollect());
        }

        // 查询视频标签
        List<VideoTagPo> tagPoList = videoRepository.queryVideoTags(videoId);
        if (CollectionUtils.isNotEmpty(tagPoList)) {
            videoPlayDetailVo.setTagList(tagPoList.stream()
                    .map(VideoTagPo::getTagName)
                    .collect(Collectors.toList()));
        }

        UserPo userPo = userRepository.queryUserById(videoPo.getUserId());
        if (userPo != null) {
            videoPlayDetailVo.setUsername(userPo.getUsername());
            videoPlayDetailVo.setAvatarUrl(userPo.getAvatarUrl());
            videoPlayDetailVo.setUserBio(userPo.getBio());
        }

        UserStatsPo userStatsPo = userRepository.queryUserStatsById(videoPo.getUserId());
        if (userStatsPo != null) {
            videoPlayDetailVo.setFansCount(userStatsPo.getFansNum());
        }

        if (myId != null) {
            FollowBo followBo = interactRelationService.queryFollowRelation(myId, videoPo.getUserId());
            videoPlayDetailVo.setIsFollow(followBo.getIsFollow());
        }

        return videoPlayDetailVo;
    }

    private Long getCurrentUserIdOrNull() {
        return UserHolderUtil.getUser() == null ? null : UserHolderUtil.getUser().getUserId();
    }

    private String loadPartitionName(Long partitionId) {
        if (partitionId == null) {
            return null;
        }
        VideoPartitionPo partitionPo = videoPartitionMapper.selectById(partitionId);
        return partitionPo == null ? null : partitionPo.getPartitionName();
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

    @Override
    public List<SearchVideoVo> getVideoPlayList(Long lastVideoId) {
        List<VideoEsDoc> docs = searchRepository.listVideoPlayList(lastVideoId, HOME_PAGE_VIDEO_LIST_SIZE);
        return docs.stream().map(searchConverter::toSearchVideoVo).collect(Collectors.toList());
    }

    @Override
    public List<UserVideoHistoryVo> listUserVideoHistory(Integer pageNum, Integer pageSize) {
        Long userId = UserHolderUtil.getUser().getUserId();
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null ? 20 : Math.min(Math.max(pageSize, 1), 50);

        List<UserVideoHistoryPo> historyList = videoRepository.batchQueryUserVideoHistory(userId, safePageNum, safePageSize);
        if (CollectionUtils.isEmpty(historyList)) {
            return Collections.emptyList();
        }

        List<UserVideoHistoryVo> result = new ArrayList<>();
        for (UserVideoHistoryPo historyPo : historyList) {
            if (historyPo == null || historyPo.getVideoId() == null) {
                continue;
            }

            VideoPo videoPo = videoRepository.queryVideoById(historyPo.getVideoId());
            if (videoPo == null) {
                continue;
            }

            UserPo userPo = userRepository.queryUserById(videoPo.getUserId());
            result.add(UserVideoHistoryVo.builder()
                    .videoId(videoPo.getId())
                    .title(videoPo.getTitle())
                    .userId(videoPo.getUserId())
                    .username(userPo == null ? "未知用户" : userPo.getUsername())
                    .coverUrl(videoPo.getCoverUrl())
                    .duration(videoPo.getDuration() == null ? historyPo.getDuration() : videoPo.getDuration())
                    .lastPlayTime(historyPo.getLastPlayTime())
                    .isFinished(historyPo.getIsFinished())
                    .historyTime(historyPo.getUpdateTime())
                    .build());
        }
        return result;
    }

    @Override
    public PageVo<CreatorVideoManageVo> listCreatorVideos(CreatorVideoQueryRequest request) {
        Long userId = UserHolderUtil.getUser().getUserId();
        int safePageNum = request == null || request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
        int safePageSize = request == null || request.getPageSize() == null ? 12 : Math.min(Math.max(request.getPageSize(), 1), 50);
        String keyword = request == null ? null : request.getKeyword();
        Integer status = request == null ? null : request.getStatus();

        Page<VideoPo> page = videoRepository.queryCreatorVideoPage(userId, keyword, status, safePageNum, safePageSize);
        List<VideoPo> videoRecords = page.getRecords();
        if (CollectionUtils.isEmpty(videoRecords)) {
            return PageVo.<CreatorVideoManageVo>builder()
                    .total(page.getTotal())
                    .pageNum(safePageNum)
                    .pageSize(safePageSize)
                    .isEnd(true)
                    .records(Collections.emptyList())
                    .build();
        }

        List<Long> videoIds = videoRecords.stream().map(VideoPo::getId).collect(Collectors.toList());
        Map<Long, VideoStatsPo> statsMap = videoStatsMapper.selectList(new LambdaQueryWrapper<VideoStatsPo>()
                        .in(VideoStatsPo::getVideoId, videoIds)
                        .eq(VideoStatsPo::getIsDeleted, 0))
                .stream()
                .collect(Collectors.toMap(VideoStatsPo::getVideoId, item -> item, (left, right) -> left));

        Set<Long> partitionIds = videoRecords.stream()
                .map(VideoPo::getPartitionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> partitionNameMap = partitionIds.isEmpty()
                ? Collections.emptyMap()
                : videoPartitionMapper.selectBatchIds(partitionIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(VideoPartitionPo::getId, VideoPartitionPo::getPartitionName, (left, right) -> left));

        List<CreatorVideoManageVo> records = videoRecords.stream()
                .map(video -> {
                    VideoStatsPo statsPo = statsMap.get(video.getId());
                    return CreatorVideoManageVo.builder()
                            .videoId(video.getId())
                            .title(video.getTitle())
                            .description(video.getDescription())
                            .coverUrl(video.getCoverUrl())
                            .duration(video.getDuration())
                            .partitionId(video.getPartitionId())
                            .partitionName(partitionNameMap.get(video.getPartitionId()))
                            .status(video.getStatus())
                            .statusText(resolveVideoStatusText(video.getStatus()))
                            .playCount(statsPo == null ? 0L : defaultLong(statsPo.getPlayCount()))
                            .likeCount(statsPo == null ? 0L : defaultLong(statsPo.getLikeCount()))
                            .collectCount(statsPo == null ? 0L : defaultLong(statsPo.getCollectCount()))
                            .commentCount(statsPo == null ? 0L : defaultLong(statsPo.getCommentCount()))
                            .createTime(video.getCreateTime())
                            .updateTime(video.getUpdateTime())
                            .build();
                })
                .collect(Collectors.toList());

        return PageVo.<CreatorVideoManageVo>builder()
                .total(page.getTotal())
                .pageNum(safePageNum)
                .pageSize(safePageSize)
                .isEnd((long) safePageNum * safePageSize >= page.getTotal())
                .records(records)
                .build();
    }

    private String resolveVideoStatusText(Integer status) {
        if (status == null) {
            return "未知状态";
        }
        return Arrays.stream(VideoStatusEnum.values())
                .filter(item -> item.getCode() == status)
                .findFirst()
                .map(VideoStatusEnum::getMessage)
                .orElse("未知状态");
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }
}
