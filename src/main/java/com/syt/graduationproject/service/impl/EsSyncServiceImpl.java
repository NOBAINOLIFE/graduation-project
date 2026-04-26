package com.syt.graduationproject.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.syt.graduationproject.enums.UserStatusEnum;
import com.syt.graduationproject.enums.VideoStatusEnum;
import com.syt.graduationproject.mapper.VideoMapper;
import com.syt.graduationproject.mapper.VideoPartitionMapper;
import com.syt.graduationproject.model.es.UserEsDoc;
import com.syt.graduationproject.model.es.VideoEsDoc;
import com.syt.graduationproject.model.po.UserPo;
import com.syt.graduationproject.model.po.UserStatsPo;
import com.syt.graduationproject.model.po.VideoPartitionPo;
import com.syt.graduationproject.model.po.VideoPo;
import com.syt.graduationproject.model.po.VideoStatsPo;
import com.syt.graduationproject.model.po.VideoTagPo;
import com.syt.graduationproject.repository.SearchRepository;
import com.syt.graduationproject.repository.UserRepository;
import com.syt.graduationproject.repository.VideoRepository;
import com.syt.graduationproject.service.EsSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EsSyncServiceImpl implements EsSyncService {

    private final SearchRepository searchRepository;

    private final UserRepository userRepository;

    private final VideoRepository videoRepository;

    private final VideoMapper videoMapper;

    private final VideoPartitionMapper videoPartitionMapper;

    @Override
    public void syncUser(Long userId) {
        if (userId == null) {
            return;
        }

        runSafely("同步用户 ES 文档", userId, () -> {
            UserPo userPo = userRepository.queryUserAnyStatusById(userId);
            if (userPo == null || !Objects.equals(userPo.getStatus(), UserStatusEnum.NORMAL.getCode())) {
                searchRepository.deleteUserDoc(userId);
                return;
            }

            UserStatsPo userStatsPo = userRepository.queryUserStatsById(userId);
            UserEsDoc userEsDoc = UserEsDoc.builder()
                    .userId(userPo.getId())
                    .username(userPo.getUsername())
                    .avatar(userPo.getAvatarUrl())
                    .fansCount(userStatsPo == null || userStatsPo.getFansNum() == null ? 0L : userStatsPo.getFansNum())
                    .videoCount(userStatsPo == null || userStatsPo.getVideoNum() == null ? 0L : userStatsPo.getVideoNum())
                    .bio(userPo.getBio())
                    .build();
            searchRepository.upsertUserDoc(userEsDoc);
        });
    }

    @Override
    public void deleteUser(Long userId) {
        if (userId == null) {
            return;
        }
        runSafely("删除用户 ES 文档", userId, () -> searchRepository.deleteUserDoc(userId));
    }

    @Override
    public void syncVideo(Long videoId) {
        if (videoId == null) {
            return;
        }

        runSafely("同步视频 ES 文档", videoId, () -> {
            VideoPo videoPo = videoRepository.queryVideoById(videoId);
            if (videoPo == null || !Objects.equals(videoPo.getStatus(), VideoStatusEnum.PUBLISHED.getCode())) {
                searchRepository.deleteVideoDoc(videoId);
                return;
            }

            UserPo userPo = userRepository.queryUserAnyStatusById(videoPo.getUserId());
            VideoStatsPo videoStatsPo = videoRepository.queryVideoStatsById(videoId);
            VideoPartitionPo partitionPo = videoPo.getPartitionId() == null
                    ? null
                    : videoPartitionMapper.selectById(videoPo.getPartitionId());
            List<VideoTagPo> tagPoList = videoRepository.queryVideoTags(videoId);
            List<String> tagList = tagPoList == null
                    ? Collections.emptyList()
                    : tagPoList.stream()
                    .map(VideoTagPo::getTagName)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            VideoEsDoc videoEsDoc = VideoEsDoc.builder()
                    .videoId(videoPo.getId())
                    .title(videoPo.getTitle())
                    .description(videoPo.getDescription())
                    .userId(videoPo.getUserId())
                    .username(userPo == null ? "" : userPo.getUsername())
                    .coverUrl(videoPo.getCoverUrl())
                    .partitionId(videoPo.getPartitionId())
                    .partitionName(partitionPo == null ? "" : partitionPo.getPartitionName())
                    .tagList(tagList)
                    .playCount(videoStatsPo == null || videoStatsPo.getPlayCount() == null ? 0L : videoStatsPo.getPlayCount())
                    .createTime(videoPo.getCreateTime())
                    .collectionCount(videoStatsPo == null || videoStatsPo.getCollectCount() == null ? 0L : videoStatsPo.getCollectCount())
                    .duration(videoPo.getDuration())
                    .build();
            searchRepository.upsertVideoDoc(videoEsDoc);
        });
    }

    @Override
    public void deleteVideo(Long videoId) {
        if (videoId == null) {
            return;
        }
        runSafely("删除视频 ES 文档", videoId, () -> searchRepository.deleteVideoDoc(videoId));
    }

    @Override
    public void syncPublishedVideosByUserId(Long userId) {
        if (userId == null) {
            return;
        }

        List<VideoPo> publishedVideoList = videoMapper.selectList(new LambdaQueryWrapper<VideoPo>()
                .eq(VideoPo::getUserId, userId)
                .eq(VideoPo::getStatus, VideoStatusEnum.PUBLISHED.getCode()));
        for (VideoPo videoPo : publishedVideoList) {
            syncVideo(videoPo.getId());
        }
    }

    private void runSafely(String action, Long targetId, Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.error("{}失败，targetId: {}", action, targetId, e);
        }
    }
}
