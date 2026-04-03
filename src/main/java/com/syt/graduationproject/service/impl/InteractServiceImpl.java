package com.syt.graduationproject.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.syt.graduationproject.mapper.CommentMapper;
import com.syt.graduationproject.mapper.CommentStatsMapper;
import com.syt.graduationproject.mapper.FollowRecordMapper;
import com.syt.graduationproject.mapper.VideoStatsMapper;
import com.syt.graduationproject.model.bo.FollowBo;
import com.syt.graduationproject.model.po.CommentPo;
import com.syt.graduationproject.model.po.CommentStatsPo;
import com.syt.graduationproject.model.po.FollowRecordPo;
import com.syt.graduationproject.model.request.CommentRequest;
import com.syt.graduationproject.model.request.FollowRequest;
import com.syt.graduationproject.repository.InteractRepository;
import com.syt.graduationproject.service.InteractService;
import com.syt.graduationproject.util.UserHolderUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

import static com.syt.graduationproject.constant.CommonConstant.NOT_DELETED;

@Service
@RequiredArgsConstructor
public class InteractServiceImpl implements InteractService {

    private final InteractRepository interactRepository;

    private final FollowRecordMapper followRecordMapper;

    private final CommentMapper commentMapper;

    private final CommentStatsMapper commentStatsMapper;

    private final VideoStatsMapper videoStatsMapper;

    /**
     * 查询两者关注关系
     */
    @Override
    public FollowBo queryFollow(Long followerId, Long followeeId) {
        FollowBo followBo = new FollowBo();

        // 1. 查询当前用户(followerId)是否关注了目标用户(followeeId)
        FollowRecordPo followRecordPo1 = interactRepository.queryFollow(followerId, followeeId);
        followBo.setIsFollow(followRecordPo1.getIsDeleted().equals(NOT_DELETED));

        // 2. 查询目标用户(followeeId)是否关注了当前用户(followerId)
        FollowRecordPo followRecordPo2 = interactRepository.queryFollow(followeeId, followerId);
        followBo.setIsFans(followRecordPo2.getIsDeleted().equals(NOT_DELETED));

        return followBo;
    }

    /**
     * 关注/取关
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void follow(FollowRequest request) {
        Long myId = UserHolderUtil.getUser().getUserId();
        Long followeeId = request.getFolloweeId();
        Integer operation = request.getOperation(); // 1: 关注, 0: 取关

        // 查询记录
        LambdaQueryWrapper<FollowRecordPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FollowRecordPo::getFollowerId, myId)
                .eq(FollowRecordPo::getFolloweeId, followeeId);
        FollowRecordPo oldRecord = followRecordMapper.selectOne(wrapper);

        if (operation == 1) {
            // --- 关注逻辑 ---
            if (Objects.isNull(oldRecord)) {
                // 数据库没记录：直接插入
                FollowRecordPo record = new FollowRecordPo();
                record.setFollowerId(myId);
                record.setFolloweeId(followeeId);
                followRecordMapper.insert(record);
            } else if (oldRecord.getIsDeleted() == 1) {
                // 数据库有记录但已取关：手动把 1 改回 0
                oldRecord.setIsDeleted(0);
                followRecordMapper.updateById(oldRecord);
            }
            // 更新关注数和粉丝数
            interactRepository.updateUserFollowNum(myId, 1L);
            interactRepository.updateUserFansNum(followeeId, 1L);
        } else {
            // --- 取关逻辑 ---
            // 只有当前是关注状态，才执行逻辑删除
            if (Objects.nonNull(oldRecord) && oldRecord.getIsDeleted() == 0) {
                oldRecord.setIsDeleted(1);
                followRecordMapper.updateById(oldRecord);
                // 更新关注数和粉丝数
                interactRepository.updateUserFollowNum(myId, -1L);
                interactRepository.updateUserFansNum(followeeId, -1L);
            }
        }
    }

    /**
     * 评论
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void comment(CommentRequest request) {
        Long myId = UserHolderUtil.getUser().getUserId();
        // 1. 构造并插入评论主表
        CommentPo comment = CommentPo.builder()
                .userId(myId)
                .videoId(request.getVideoId())
                .content(request.getContent())
                .rootId(request.getRootId())
                .parentId(Optional.ofNullable(request.getParentId()).orElse(0L))
                .replyUserId(Optional.ofNullable(request.getReplyUserId()).orElse(0L))
                .build();
        commentMapper.insert(comment);

        // 2. 为每一条新评论初始化统计行
        CommentStatsPo commentStatsPo = CommentStatsPo.builder()
                .commentId(comment.getId())
                .likeCount(0L)
                .replyCount(0L)
                .build();
        commentStatsMapper.insert(commentStatsPo);

        // 3. 维护层级计数逻辑
        if (comment.getRootId() != 0) {
            // 如果是回复（子评论），给它的“根评论”回复数 +1
            commentStatsMapper.incrReplyCount(comment.getRootId());
        }

        // 4. 更新视频维度的总评论数
        if (request.getRootId() == 0) {
            videoStatsMapper.incrCommentCount(comment.getVideoId());
        }
    }
}
