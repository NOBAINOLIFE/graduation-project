package com.syt.graduationproject.service.impl;

import com.syt.graduationproject.model.bo.FollowBo;
import com.syt.graduationproject.model.bo.UserVideoInteractionBo;
import com.syt.graduationproject.model.po.CollectionItemPo;
import com.syt.graduationproject.model.po.FollowRecordPo;
import com.syt.graduationproject.model.po.LikeCommentPo;
import com.syt.graduationproject.model.po.LikeVideoPo;
import com.syt.graduationproject.repository.InteractRelationRepository;
import com.syt.graduationproject.service.InteractRelationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.syt.graduationproject.constant.CommonConstant.NOT_DELETED;

/**
 * 互动关系查询服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InteractRelationServiceImpl implements InteractRelationService {

    private final InteractRelationRepository interactRelationRepository;

    /**
     * 查询两个用户之间的关注关系
     */
    @Override
    public FollowBo queryFollowRelation(Long followerId, Long followeeId) {
        FollowBo followBo = new FollowBo();

        FollowRecordPo followRecordPo1 = interactRelationRepository.queryFollowRecord(followerId, followeeId);
        followBo.setIsFollow(followRecordPo1 != null && followRecordPo1.getIsDeleted().equals(NOT_DELETED));

        FollowRecordPo followRecordPo2 = interactRelationRepository.queryFollowRecord(followeeId, followerId);
        followBo.setIsFans(followRecordPo2 != null && followRecordPo2.getIsDeleted().equals(NOT_DELETED));

        return followBo;
    }

    /**
     * 查询用户是否点赞了指定视频
     */
    @Override
    public boolean isLikeVideo(Long userId, Long videoId) {
        if (userId == null || videoId == null) {
            return false;
        }
        LikeVideoPo likeVideo = interactRelationRepository.queryLikeVideo(userId, videoId);
        return likeVideo != null;
    }

    /**
     * 查询用户是否收藏了指定视频
     */
    @Override
    public boolean isCollectVideo(Long userId, Long videoId) {
        if (userId == null || videoId == null) {
            return false;
        }
        List<CollectionItemPo> collectedItems = interactRelationRepository.queryCollectVideo(userId, videoId);
        return CollectionUtils.isNotEmpty(collectedItems);
    }

    /**
     * 查询用户对视频的投币数量
     */
    @Override
    public Integer getUserCoinCountForVideo(Long userId, Long videoId) {
        if (userId == null || videoId == null) {
            return 0;
        }
        return interactRelationRepository.queryUserCoinCountForVideo(userId, videoId);
    }

    /**
     * 批量查询用户对多条评论的点赞记录
     */
    @Override
    public Set<Long> batchQueryLikeComment(Long userId, List<Long> commentIdList) {
        if (userId == null || CollectionUtils.isEmpty(commentIdList)) {
            return Collections.emptySet();
        }
        List<LikeCommentPo> likeCommentPos = interactRelationRepository.batchQueryLikeComment(userId, commentIdList);
        return likeCommentPos.stream()
                .map(LikeCommentPo::getCommentId)
                .collect(Collectors.toSet());
    }

    /**
     * 查询用户与视频的完整交互信息
     */
    @Override
    public UserVideoInteractionBo queryUserVideoInteraction(Long userId, Long videoId) {
        if (userId == null || videoId == null) {
            return UserVideoInteractionBo.builder()
                    .isLike(false)
                    .isCollect(false)
                    .coinCount(0)
                    .build();
        }

        boolean isLiked = isLikeVideo(userId, videoId);
        boolean isCollected = isCollectVideo(userId, videoId);
        Integer coinAmount = getUserCoinCountForVideo(userId, videoId);

        return UserVideoInteractionBo.builder()
                .isLike(isLiked)
                .isCollect(isCollected)
                .coinCount(coinAmount)
                .build();
    }
}
