package com.syt.graduationproject.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.syt.graduationproject.enums.*;
import com.syt.graduationproject.exception.ErrorOperationException;
import com.syt.graduationproject.exception.ErrorParamException;
import com.syt.graduationproject.exception.NoPermissionException;
import com.syt.graduationproject.exception.NotFoundException;
import com.syt.graduationproject.mapper.*;
import com.syt.graduationproject.model.es.UserEsDoc;
import com.syt.graduationproject.model.es.VideoEsDoc;
import com.syt.graduationproject.model.po.*;
import com.syt.graduationproject.model.request.*;
import com.syt.graduationproject.model.vo.*;
import com.syt.graduationproject.model.vo.Page.CommentPageVo;
import com.syt.graduationproject.model.vo.Page.PageVo;
import com.syt.graduationproject.model.vo.report.ManagerReportRecordVo;
import com.syt.graduationproject.model.websocket.*;
import com.syt.graduationproject.converter.SearchConverter;
import com.syt.graduationproject.repository.InteractRepository;
import com.syt.graduationproject.repository.SearchRepository;
import com.syt.graduationproject.repository.VideoRepository;
import com.syt.graduationproject.service.EsSyncService;
import com.syt.graduationproject.service.InteractRelationService;
import com.syt.graduationproject.service.InteractService;
import com.syt.graduationproject.service.SearchService;
import com.syt.graduationproject.util.JsonUtil;
import com.syt.graduationproject.util.RedisKeyUtil;
import com.syt.graduationproject.util.UserHolderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import static com.syt.graduationproject.constant.CommonConstant.DELETED;
import static com.syt.graduationproject.constant.CommonConstant.NOT_DELETED;
import static com.syt.graduationproject.repository.impl.SearchRepositoryImpl.USER_INDEX;
import static com.syt.graduationproject.repository.impl.SearchRepositoryImpl.VIDEO_INDEX;

@Slf4j
@Service
@RequiredArgsConstructor
public class InteractServiceImpl implements InteractService {

    private final InteractRepository interactRepository;

    private final FollowRecordMapper followRecordMapper;

    private final CommentMapper commentMapper;

    private final CommentStatsMapper commentStatsMapper;

    private final VideoStatsMapper videoStatsMapper;

    private final LikeVideoMapper likeVideoMapper;

    private final LikeCommentMapper likeCommentMapper;

    private final CollectionDirectoryMapper collectionDirectoryMapper;

    private final CollectionItemMapper collectionItemMapper;

    private final UserBlockMapper userBlockMapper;

    private final UserWalletMapper userWalletMapper;

    private final UserCoinChangeLogMapper userCoinChangeLogMapper;

    private final PrivateMessageMapper privateMessageMapper;

    private final ReportMapper reportMapper;

    private final VideoMapper videoMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final VideoRepository videoRepository;

    private final EsSyncService esSyncService;

    private final SearchService searchService;

    private final InteractRelationService interactRelationService;

    /**
     * 在线私聊会话：userId -> sessions（多端登录）
     */
    private final Map<Long, Set<WebSocketSession>> chatSessions = new ConcurrentHashMap<>();

    /**
     * 心跳时间：sessionId -> lastSeenMillis
     */
    private final Map<String, Long> sessionLastSeen = new ConcurrentHashMap<>();

    /**
     * 在线映射在 Redis 的 TTL（心跳会刷新）
     */
    private static final Duration ONLINE_TTL = Duration.ofSeconds(120);

    /**
     * 分享去重缓存 TTL：缓存过期后回查数据库并重建
     */
    private static final Duration VIDEO_SHARE_DEDUP_TTL = Duration.ofDays(7);

    private final UserMapper userMapper;

    private final VideoShareRecordMapper videoShareRecordMapper;

    private static final int OPERATION_ON = 1;

    private static final int OPERATION_OFF = 0;

    private static final int DAILY_LOGIN_REWARD = 1;

    private static final int COIN_CHANGE_TYPE_DAILY_REWARD = 1;

    private static final int COIN_CHANGE_TYPE_VIDEO_REWARD = 2;

    private static final String DEFAULT_COLLECTION_NAME = "默认收藏夹";

    /**
     * 关注/取关
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void follow(FollowRequest request) {
        Long myId = UserHolderUtil.getUser().getUserId();
        Long followeeId = request.getFolloweeId();
        Integer operation = request.getOperation(); // 1: 关注, 0: 取关
        boolean fansChanged = false;
        if (followeeId == null || operation == null) {
            throw new ErrorParamException("关注参数不完整");
        }
        if (Objects.equals(myId, followeeId)) {
            throw new ErrorOperationException("不能关注自己");
        }
        if (operation != OPERATION_ON && operation != OPERATION_OFF) {
            throw new ErrorParamException("关注操作类型非法");
        }
        if (hasMutualBlock(myId, followeeId)) {
            throw new ErrorOperationException("由于隐私设置，无法关注该用户");
        }

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
            fansChanged = true;
        } else {
            // --- 取关逻辑 ---
            // 只有当前是关注状态，才执行逻辑删除
            if (Objects.nonNull(oldRecord) && oldRecord.getIsDeleted() == 0) {
                oldRecord.setIsDeleted(1);
                followRecordMapper.updateById(oldRecord);
                // 更新关注数和粉丝数
                interactRepository.updateUserFollowNum(myId, -1L);
                interactRepository.updateUserFansNum(followeeId, -1L);
                fansChanged = true;
            }
        }

        if (fansChanged) {
            esSyncService.syncUser(followeeId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void blockUser(BlockUserRequest request) {
        Long myId = UserHolderUtil.getUser().getUserId();
        if (request == null || request.getTargetUserId() == null || request.getOperation() == null) {
            throw new ErrorParamException("拉黑参数不完整");
        }
        Long targetUserId = request.getTargetUserId();
        if (Objects.equals(myId, targetUserId)) {
            throw new ErrorOperationException("不能拉黑自己");
        }
        UserPo targetUser = userMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new NotFoundException("用户不存在");
        }
        if (request.getOperation() != OPERATION_ON && request.getOperation() != OPERATION_OFF) {
            throw new ErrorParamException("拉黑操作类型非法");
        }

        LambdaQueryWrapper<UserBlockPo> wrapper = new LambdaQueryWrapper<UserBlockPo>()
                .eq(UserBlockPo::getUserId, myId)
                .eq(UserBlockPo::getBlockedUserId, targetUserId);
        UserBlockPo old = userBlockMapper.selectOne(wrapper);

        if (request.getOperation() == OPERATION_ON) {
            if (old == null) {
                userBlockMapper.insert(UserBlockPo.builder()
                        .userId(myId)
                        .blockedUserId(targetUserId)
                        .isDeleted(NOT_DELETED)
                        .build());
            } else if (!NOT_DELETED.equals(old.getIsDeleted())) {
                old.setIsDeleted(NOT_DELETED);
                userBlockMapper.updateById(old);
            }
            // 拉黑后自动互相取关，避免后续可见性和关系异常。
            cancelFollowIfPresent(myId, targetUserId);
            cancelFollowIfPresent(targetUserId, myId);
            return;
        }

        if (old != null && NOT_DELETED.equals(old.getIsDeleted())) {
            old.setIsDeleted(1);
            userBlockMapper.updateById(old);
        }
    }

    @Override
    public List<UserSimpleInfoVo> queryMyBlockList() {
        Long myId = UserHolderUtil.getUser().getUserId();
        List<UserBlockPo> blockRecords = userBlockMapper.selectList(new LambdaQueryWrapper<UserBlockPo>()
                .eq(UserBlockPo::getUserId, myId)
                .eq(UserBlockPo::getIsDeleted, NOT_DELETED)
                .orderByDesc(UserBlockPo::getCreateTime)
                .orderByDesc(UserBlockPo::getId));
        if (CollectionUtils.isEmpty(blockRecords)) {
            return Collections.emptyList();
        }

        List<Long> blockedUserIds = blockRecords.stream()
                .map(UserBlockPo::getBlockedUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, UserPo> userMap = queryUserMap(blockedUserIds);
        Set<Long> followedUserIds = queryFolloweeIdSet(myId, blockedUserIds);
        Set<Long> fanUserIds = queryFollowerIdSet(myId, blockedUserIds);
        List<UserSimpleInfoVo> result = new ArrayList<>();
        for (Long blockedUserId : blockedUserIds) {
            UserPo userPo = userMap.get(blockedUserId);
            if (userPo == null) {
                continue;
            }
            result.add(UserSimpleInfoVo.builder()
                    .userId(userPo.getId())
                    .username(userPo.getUsername())
                    .avatarUrl(userPo.getAvatarUrl())
                    .bio(userPo.getBio())
                    .isFollow(followedUserIds.contains(blockedUserId))
                    .isFans(fanUserIds.contains(blockedUserId))
                    .build());
        }
        return result;
    }

    @Override
    public boolean hasMutualBlock(Long userA, Long userB) {
        if (userA == null || userB == null) {
            return false;
        }
        return userBlockMapper.countAnyDirectionBlock(userA, userB) > 0;
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
                .videoId(request.getVideoId())
                .commentId(comment.getId())
                .likeCount(0L)
                .replyCount(0L)
                .hotScore(10L)
                .build();
        commentStatsMapper.insert(commentStatsPo);

        // 3. 维护层级计数逻辑
        if (comment.getRootId() != 0) {
            // 根评论维护整棵回复树的总回复数
            commentStatsMapper.incrReplyCount(comment.getRootId());
            // 被直接回复的子评论维护自己的 direct reply_count，用于热度计算
            if (comment.getParentId() != null
                    && comment.getParentId() > 0
                    && !Objects.equals(comment.getParentId(), comment.getRootId())) {
                commentStatsMapper.updateReplyCount(comment.getParentId(), 1);
            }
        }

        // 4. 更新视频维度的总评论数
        if (request.getRootId() == 0) {
            videoStatsMapper.incrCommentCount(comment.getVideoId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId) {
        Long currentUserId = UserHolderUtil.getUser().getUserId();
        if (commentId == null) {
            throw new ErrorParamException("评论ID不能为空");
        }

        CommentPo commentPo = commentMapper.selectById(commentId);
        if (commentPo == null || !Objects.equals(commentPo.getIsDeleted(), NOT_DELETED)) {
            throw new NotFoundException("评论不存在");
        }

        VideoPo videoPo = videoMapper.selectById(commentPo.getVideoId());
        boolean canDelete = Objects.equals(commentPo.getUserId(), currentUserId)
                || (videoPo != null && Objects.equals(videoPo.getUserId(), currentUserId));
        if (!canDelete) {
            throw new NoPermissionException("无权限删除该评论");
        }

        doDeleteComment(commentPo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void topComment(CommentTopRequest request) {
        Long currentUserId = UserHolderUtil.getUser().getUserId();
        if (request == null || request.getCommentId() == null) {
            throw new ErrorParamException("评论ID不能为空");
        }

        CommentPo commentPo = commentMapper.selectById(request.getCommentId());
        if (commentPo == null || !Objects.equals(commentPo.getIsDeleted(), NOT_DELETED)) {
            throw new NotFoundException("评论不存在");
        }
        if (!Objects.equals(commentPo.getRootId(), 0L)) {
            throw new ErrorOperationException("仅支持置顶主评论");
        }

        VideoPo videoPo = videoMapper.selectById(commentPo.getVideoId());
        if (videoPo == null || !Objects.equals(videoPo.getUserId(), currentUserId)) {
            throw new NoPermissionException("只有视频作者可以置顶评论");
        }

        Integer operation = Optional.ofNullable(request.getOperation()).orElse(1);
        if (Objects.equals(operation, 1)) {
            commentMapper.clearTopByVideoId(commentPo.getVideoId());
            commentMapper.updateTopById(commentPo.getId(), 1);
            return;
        }
        commentMapper.updateTopById(commentPo.getId(), 0);
    }

    /**
     * 点赞/取消点赞视频
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeVideo(LikeRequest request) {
        Long myId = UserHolderUtil.getUser().getUserId();
        Long videoId = request.getTargetId();
        Integer operation = request.getOperation();
        VideoPo videoPo = videoMapper.selectById(videoId);
        if (videoPo == null) {
            throw new NotFoundException("视频不存在");
        }
        if (!videoPo.getStatus().equals(VideoStatusEnum.PUBLISHED.getCode())) {
            throw new ErrorOperationException("视频状态异常");
        }

        if (operation == 1) {
            // --- 执行点赞 ---
            LikeVideoPo likeVideoPo = LikeVideoPo.builder().userId(myId).videoId(videoId).build();
            try {
                likeVideoMapper.insert(likeVideoPo);
                // 插入成功，增加计数
                videoStatsMapper.updateLikeCount(videoId, 1);
                interactRepository.updateUserLikeNum(videoPo.getUserId(), 1L);
            } catch (DuplicateKeyException e) {
                // 幂等处理：如果已经点过赞，直接忽略，不重复加分
                log.warn("用户重复点赞，用户ID：{}，视频ID：{}", myId, videoId);
            }
        } else if (operation == 0) {
            // --- 执行取消 ---
            QueryWrapper<LikeVideoPo> wrapper = new QueryWrapper<>();
            wrapper.lambda()
                    .eq(LikeVideoPo::getUserId, myId)
                    .eq(LikeVideoPo::getVideoId, videoId);
            int rows = likeVideoMapper.delete(wrapper);
            if (rows > 0) {
                // 只有真正删除了记录，才减少计数
                videoStatsMapper.updateLikeCount(videoId, -1);
                interactRepository.updateUserLikeNum(videoPo.getUserId(), -1L);
            }
        } else {
            throw new ErrorParamException("操作类型非法");
        }
    }

    /**
     * 点赞/取消点赞评论
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeComment(LikeRequest request) {
        Long myId = UserHolderUtil.getUser().getUserId();
        Long commentId = request.getTargetId();
        Integer operation = request.getOperation();
        if (operation == 1) {
            // --- 执行点赞 ---
            LikeCommentPo likeCommentPo = LikeCommentPo.builder().userId(myId).commentId(commentId).build();
            try {
                likeCommentMapper.insert(likeCommentPo);
                // 插入成功，增加计数
                commentStatsMapper.updateLikeCount(commentId, 1);
            } catch (DuplicateKeyException e) {
                // 幂等处理：如果已经点过赞，直接忽略，不重复加分
                log.warn("用户重复点赞，用户ID：{}，评论ID：{}", myId, commentId);
            }
        } else {
            // --- 执行取消 ---
            QueryWrapper<LikeCommentPo> wrapper = new QueryWrapper<>();
            wrapper.lambda()
                    .eq(LikeCommentPo::getUserId, myId)
                    .eq(LikeCommentPo::getCommentId, commentId);
            int rows = likeCommentMapper.delete(wrapper);
            if (rows > 0) {
                // 只有真正删除了记录，才减少计数
                commentStatsMapper.updateLikeCount(commentId, -1);
            }
        }
    }

    @Override
    public void registerChatSession(Long userId, WebSocketSession session) {
        chatSessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
        sessionLastSeen.put(session.getId(), System.currentTimeMillis());
        // 1) 写入 Redis：userId -> sessionId（用于在线判断/多实例扩展的基础）
        stringRedisTemplate.opsForValue().set(RedisKeyUtil.onlineKey(userId), session.getId(), ONLINE_TTL);
        // 2) 上线即拉取离线消息（Redis List）并顺序推送，成功投递后再更新状态并移除已投递前缀
        deliverOfflineMessages(userId);
    }

    @Override
    public void removeChatSession(Long userId, WebSocketSession session) {
        Set<WebSocketSession> set = chatSessions.get(userId);
        if (set == null) {
            return;
        }
        set.remove(session);
        sessionLastSeen.remove(session.getId());
        if (set.isEmpty()) {
            chatSessions.remove(userId);
        }
        // Redis 在线 key：只有当用户没有任何会话时才删除（避免多端误删）
        Set<WebSocketSession> still = chatSessions.get(userId);
        if (still == null || still.isEmpty()) {
            stringRedisTemplate.delete(RedisKeyUtil.onlineKey(userId));
        }
    }

    @Override
    public void sendPrivateChat(Long fromUserId, PrivateChatSendRequest request) {
        if (request == null || request.getToUserId() == null) {
            throw new IllegalArgumentException("toUserId 不能为空");
        }
        String content = Optional.ofNullable(request.getContent()).orElse("").trim();
        if (content.isEmpty()) {
            throw new IllegalArgumentException("content 不能为空");
        }

        Long toUserId = request.getToUserId();
        if (Objects.equals(fromUserId, toUserId)) {
            throw new ErrorOperationException("不能给自己发送私信");
        }
        UserPo senderUser = userMapper.selectById(fromUserId);
        UserPo targetUser = userMapper.selectById(toUserId);
        if (senderUser == null) {
            throw new NotFoundException("发送方用户不存在");
        }
        if (targetUser == null) {
            throw new NotFoundException("接收方用户不存在");
        }
        PrivateMessageFailReasonEnum failReason = resolvePrivateMessageFailReason(fromUserId, toUserId, senderUser, targetUser);
        boolean failed = !Objects.equals(failReason, PrivateMessageFailReasonEnum.NONE);

        // 1) 落库（生成 serverMsgId）——MySQL 兜底：无论在线/离线都先写入历史
        PrivateMessagePo po = PrivateMessagePo.builder()
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .clientMsgId(request.getClientMsgId())
                .content(content)
                .status(failed ? PrivateMessageStatusEnum.FAIL.getCode() : PrivateMessageStatusEnum.SAVED.getCode())
                .failReason(failReason.getCode())
                .build();
        try {
            privateMessageMapper.insert(po);
        } catch (DuplicateKeyException e) {
            // 幂等：相同 from_user_id + client_msg_id 重复发送时，直接查回已有记录并继续推送
            PrivateMessagePo existed = privateMessageMapper.selectOne(new LambdaQueryWrapper<PrivateMessagePo>()
                    .eq(PrivateMessagePo::getFromUserId, fromUserId)
                    .eq(PrivateMessagePo::getClientMsgId, request.getClientMsgId())
                    .last("limit 1"));
            if (existed != null) {
                broadcastToUser(fromUserId, WsEnvelope.builder()
                        .type("chat_send_ack")
                        .data(buildSendAckPayload(request.getClientMsgId(), existed.getId(), toUserId, existed.getStatus(), existed.getFailReason()))
                        .build());
                return;
            } else {
                throw e;
            }
        }

        Long serverMsgId = po.getId();
        if (failed) {
            broadcastToUser(fromUserId, WsEnvelope.builder()
                    .type("chat_send_ack")
                    .data(buildSendAckPayload(request.getClientMsgId(), serverMsgId, toUserId, po.getStatus(), po.getFailReason()))
                    .build());
            return;
        }

        PrivateChatMessage chatMsg = PrivateChatMessage.builder()
                .serverMsgId(serverMsgId)
                .clientMsgId(request.getClientMsgId())
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .content(content)
                .status(PrivateMessageStatusEnum.SAVED.getCode())
                .failReason(PrivateMessageFailReasonEnum.NONE.getCode())
                .failReasonText(PrivateMessageFailReasonEnum.NONE.getMessage())
                .sendTime(LocalDateTime.now())
                .build();

        WsEnvelope<PrivateChatMessage> chatEnvelope = WsEnvelope.<PrivateChatMessage>builder()
                .type("chat")
                .data(chatMsg)
                .build();

        // 2) 在线判断：优先用 Redis 判断是否在线；若在线则实时转发，否则写离线队列
        boolean isOnline = stringRedisTemplate.hasKey(RedisKeyUtil.onlineKey(toUserId));
        boolean delivered = false;
        if (isOnline) {
            delivered = broadcastToUser(toUserId, chatEnvelope);
        }
        if (!delivered) {
            // 离线：写入 Redis List（按顺序）——缓存加速
            String offlineJson = JsonUtil.toJson(chatEnvelope);
            stringRedisTemplate.opsForList().rightPush(RedisKeyUtil.offlineListKey(toUserId), offlineJson);
        }

        // 3) 更新 MySQL 状态：在线投递成功 -> DELIVERED；离线则保持“未读”（即非 READ）
        if (delivered) {
            PrivateMessagePo update = PrivateMessagePo.builder()
                    .status(PrivateMessageStatusEnum.DELIVERED.getCode())
                    .deliveredTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            privateMessageMapper.update(update, new LambdaQueryWrapper<PrivateMessagePo>()
                            .eq(PrivateMessagePo::getId, serverMsgId)
                            // 仅允许 SAVED -> DELIVERED，避免覆盖 ACKED/READ
                            .eq(PrivateMessagePo::getStatus, PrivateMessageStatusEnum.SAVED.getCode()));
        }

        // 4) 给发送方推送发送回执（包含是否实时投递成功）
        broadcastToUser(fromUserId, WsEnvelope.builder()
                .type("chat_send_ack")
                .data(buildSendAckPayload(request.getClientMsgId(), serverMsgId, toUserId, delivered ? PrivateMessageStatusEnum.DELIVERED.getCode() : PrivateMessageStatusEnum.SAVED.getCode(), PrivateMessageFailReasonEnum.NONE.getCode()))
                .build());
    }

    private PrivateMessageFailReasonEnum resolvePrivateMessageFailReason(Long fromUserId, Long toUserId, UserPo senderUser, UserPo targetUser) {
        if (Objects.equals(senderUser.getStatus(), UserStatusEnum.BANNED.getCode())) {
            return PrivateMessageFailReasonEnum.SENDER_BANNED;
        }
        if (Objects.equals(targetUser.getStatus(), UserStatusEnum.BANNED.getCode())) {
            return PrivateMessageFailReasonEnum.RECEIVER_BANNED;
        }
        if (userBlockMapper.countActiveBlock(fromUserId, toUserId) > 0) {
            return PrivateMessageFailReasonEnum.SENDER_BLOCKED_RECEIVER;
        }
        if (userBlockMapper.countActiveBlock(toUserId, fromUserId) > 0) {
            return PrivateMessageFailReasonEnum.RECEIVER_BLOCKED_SENDER;
        }
        return PrivateMessageFailReasonEnum.NONE;
    }

    private SendAckPayload buildSendAckPayload(String clientMsgId, Long serverMsgId, Long toUserId, Integer status, Integer failReason) {
        Integer safeStatus = status == null ? PrivateMessageStatusEnum.SAVED.getCode() : status;
        Integer safeFailReason = failReason == null ? PrivateMessageFailReasonEnum.NONE.getCode() : failReason;
        return SendAckPayload.builder()
                .clientMsgId(clientMsgId)
                .serverMsgId(serverMsgId)
                .toUserId(toUserId)
                .delivered(Objects.equals(safeStatus, PrivateMessageStatusEnum.DELIVERED.getCode())
                        || Objects.equals(safeStatus, PrivateMessageStatusEnum.ACKED.getCode())
                        || Objects.equals(safeStatus, PrivateMessageStatusEnum.READ.getCode()))
                .status(safeStatus)
                .failReason(safeFailReason)
                .failReasonText(getPrivateMessageFailReasonText(safeFailReason))
                .build();
    }

    private String getPrivateMessageFailReasonText(Integer code) {
        int safeCode = code == null ? PrivateMessageFailReasonEnum.NONE.getCode() : code;
        for (PrivateMessageFailReasonEnum reason : PrivateMessageFailReasonEnum.values()) {
            if (reason.getCode() == safeCode) {
                return reason.getMessage();
            }
        }
        return PrivateMessageFailReasonEnum.SYSTEM_ERROR.getMessage();
    }

    private boolean broadcastToUser(Long userId, WsEnvelope<?> envelope) {
        Set<WebSocketSession> set = chatSessions.get(userId);
        if (set == null || set.isEmpty()) {
            return false;
        }
        String json = JsonUtil.toJson(envelope);
        TextMessage textMessage = new TextMessage(json);
        boolean any = false;
        for (WebSocketSession s : set) {
            try {
                if (s != null && s.isOpen()) {
                    s.sendMessage(textMessage);
                    any = true;
                }
            } catch (Exception e) {
                log.warn("WebSocket 推送失败，userId: {}, sessionId: {}", userId, s.getId(), e);
            }
        }
        return any;
    }

    @Override
    public void ackPrivateMessage(Long userId, Long serverMsgId) {
        if (serverMsgId == null) {
            return;
        }
        LocalDateTime ackedTime = LocalDateTime.now();
        // 只有接收方才能 ack（userId == toUserId）。
        // 如果消息已经 READ，也只补齐 acked_time / delivered_time，不回退状态。
        privateMessageMapper.markAcked(
                userId,
                serverMsgId,
                PrivateMessageStatusEnum.ACKED.getCode(),
                ackedTime,
                PrivateMessageStatusEnum.SAVED.getCode(),
                PrivateMessageStatusEnum.DELIVERED.getCode(),
                PrivateMessageStatusEnum.FAIL.getCode()
        );
    }

    @Override
    public void heartbeat(Long userId, WebSocketSession session) {
        if (session != null) {
            sessionLastSeen.put(session.getId(), System.currentTimeMillis());
        }
        // 心跳刷新在线 TTL（Redis）
        if (userId != null) {
            stringRedisTemplate.expire(RedisKeyUtil.onlineKey(userId), ONLINE_TTL);
        }
    }

    /**
     * 用户上线后拉取离线消息：按顺序推送 -> 批量更新 DB 为 DELIVERED -> 移除已成功投递的队列前缀
     */
    public void deliverOfflineMessages(Long userId) {
        if (userId == null) {
            return;
        }
        String listKey = RedisKeyUtil.offlineListKey(userId);
        Long size = stringRedisTemplate.opsForList().size(listKey);
        if (size == null || size <= 0) {
            return;
        }

        List<String> items = stringRedisTemplate.opsForList().range(listKey, 0, -1);
        if (items == null || items.isEmpty()) {
            return;
        }

        List<Long> deliveredMsgIds = new ArrayList<>();
        long deliveredCount = 0L;
        LocalDateTime deliveredTime = LocalDateTime.now();
        for (String json : items) {
            if (json == null || json.trim().isEmpty()) {
                continue;
            }

            WsEnvelope<?> envelope;
            try {
                envelope = JsonUtil.fromJson(json, WsEnvelope.class);
            } catch (Exception e) {
                // 脏数据会阻塞后续消息，直接丢弃该条并继续。
                log.warn("离线消息反序列化失败，userId: {}", userId, e);
                deliveredCount++;
                continue;
            }

            // 推送给当前用户（按顺序）；一旦失败，保留当前及后续消息，等待下次重试
            boolean pushed = broadcastToUser(userId, envelope);
            if (!pushed) {
                break;
            }
            deliveredCount++;

            // 收集 serverMsgId，用于批量标记已投递
            try {
                // 先把 envelope 转成 Map 再提取 data.serverMsgId（避免泛型问题）
                @SuppressWarnings("unchecked")
                Map<String, Object> raw = (Map<String, Object>) JsonUtil.fromJson(json, Map.class);
                Object data = raw.get("data");
                if (data instanceof Map) {
                    Map<?, ?> dataMap = (Map<?, ?>) data;
                    Object serverMsgId = dataMap.get("serverMsgId");
                    if (serverMsgId != null) {
                        deliveredMsgIds.add(Long.valueOf(String.valueOf(serverMsgId)));
                    }
                }
            } catch (Exception ignore) {
            }
        }

        if (deliveredCount <= 0) {
            return;
        }

        if (!deliveredMsgIds.isEmpty()) {
            privateMessageMapper.markDeliveredByIds(
                    userId,
                    deliveredMsgIds.stream().distinct().collect(Collectors.toList()),
                    PrivateMessageStatusEnum.DELIVERED.getCode(),
                    deliveredTime,
                    PrivateMessageStatusEnum.SAVED.getCode()
            );
        }

        // 只移除已成功投递（或判定为脏数据）的前缀，未成功部分保留以便重试。
        stringRedisTemplate.opsForList().trim(listKey, deliveredCount, -1);
    }

    @Override
    public List<PrivateMessageVo> queryChatHistory(Long myId, Long withUserId, Long beforeId, Integer pageSize) {
        int size = pageSize == null ? 20 : Math.min(Math.max(pageSize, 1), 100);
        List<PrivateMessageVo> history = privateMessageMapper.queryHistory(myId, withUserId, beforeId, size);
        if (CollectionUtils.isNotEmpty(history)) {
            history.forEach(item -> item.setFailReasonText(getPrivateMessageFailReasonText(item.getFailReason())));
        }
        return history;
    }

    @Override
    public List<ChatSessionVo> queryChatSessions(Long myId) {
        List<ChatSessionVo> sessions = privateMessageMapper.querySessions(
                myId,
                PrivateMessageStatusEnum.READ.getCode(),
                PrivateMessageStatusEnum.FAIL.getCode());
        if (sessions == null || sessions.isEmpty()) {
            return sessions;
        }

        Map<Long, UserPo> userMap = queryUserMap(sessions.stream()
                .map(ChatSessionVo::getWithUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList()));
        sessions.forEach(item -> {
            UserPo userPo = userMap.get(item.getWithUserId());
            if (userPo != null) {
                item.setWithUsername(userPo.getUsername());
                item.setWithAvatarUrl(userPo.getAvatarUrl());
                item.setWithUserBanned(Objects.equals(userPo.getStatus(), UserStatusEnum.BANNED.getCode()));
            }
            item.setWithUserBlack(hasMutualBlock(myId, item.getWithUserId()));
        });
        return sessions;
    }

    @Override
    public Long queryTotalUnread(Long myId) {
        return privateMessageMapper.queryTotalUnread(myId, PrivateMessageStatusEnum.READ.getCode(), PrivateMessageStatusEnum.FAIL.getCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markChatRead(Long myId, Long withUserId, Long upToMsgId) {
        int rows = privateMessageMapper.markRead(
                myId,
                withUserId,
                upToMsgId,
                PrivateMessageStatusEnum.READ.getCode(),
                LocalDateTime.now()
        );
        if (rows > 0) {
            WsEnvelope<Object> readEvent = WsEnvelope.builder()
                    .type("read")
                    .data(new ReadPayload(myId, upToMsgId))
                    .build();
            broadcastToUser(withUserId, readEvent);
        }
        return rows;
    }

    @Override
    public List<ReplyMessageVo> queryReplyMessages() {
        Long myId = UserHolderUtil.getUser().getUserId();
        List<CommentPo> replyComments = commentMapper.selectList(new LambdaQueryWrapper<CommentPo>()
                .eq(CommentPo::getReplyUserId, myId)
                .ne(CommentPo::getUserId, myId)
                .eq(CommentPo::getIsDeleted, NOT_DELETED)
                .orderByDesc(CommentPo::getCreateTime)
                .orderByDesc(CommentPo::getId)
                .last("limit 100"));
        if (CollectionUtils.isEmpty(replyComments)) {
            return Collections.emptyList();
        }

        Set<Long> replierUserIds = replyComments.stream()
                .map(CommentPo::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> originalCommentIds = replyComments.stream()
                .map(CommentPo::getParentId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Set<Long> videoIds = replyComments.stream()
                .map(CommentPo::getVideoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, UserPo> userMap = replierUserIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(replierUserIds).stream()
                .collect(Collectors.toMap(UserPo::getId, item -> item, (left, right) -> left));
        Map<Long, CommentPo> originalCommentMap = originalCommentIds.isEmpty()
                ? Collections.emptyMap()
                : commentMapper.selectBatchIds(originalCommentIds).stream()
                .collect(Collectors.toMap(CommentPo::getId, item -> item, (left, right) -> left));
        Map<Long, VideoPo> videoMap = videoIds.isEmpty()
                ? Collections.emptyMap()
                : videoMapper.selectBatchIds(videoIds).stream()
                .collect(Collectors.toMap(VideoPo::getId, item -> item, (left, right) -> left));

        return replyComments.stream().map(comment -> {
            UserPo replier = userMap.get(comment.getUserId());
            CommentPo originalComment = originalCommentMap.get(comment.getParentId());
            VideoPo videoPo = videoMap.get(comment.getVideoId());
            return ReplyMessageVo.builder()
                    .replyCommentId(comment.getId())
                    .replierUserId(comment.getUserId())
                    .replierUsername(replier == null ? "未知用户" : replier.getUsername())
                    .replierAvatarUrl(replier == null ? null : replier.getAvatarUrl())
                    .replyContent(comment.getContent())
                    .originalCommentId(originalComment == null ? comment.getParentId() : originalComment.getId())
                    .originalCommentContent(originalComment == null ? "" : originalComment.getContent())
                    .videoId(comment.getVideoId())
                    .videoTitle(videoPo == null ? "" : videoPo.getTitle())
                    .createTime(comment.getCreateTime())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<LikeReceivedSummaryVo> queryLikeReceivedSummaries() {
        Long myId = UserHolderUtil.getUser().getUserId();
        List<LikeReceivedSummaryVo> summaryList = new ArrayList<>();

        List<CommentPo> myComments = commentMapper.selectList(new LambdaQueryWrapper<CommentPo>()
                .eq(CommentPo::getUserId, myId)
                .eq(CommentPo::getIsDeleted, NOT_DELETED));
        if (CollectionUtils.isNotEmpty(myComments)) {
            Map<Long, CommentPo> commentMap = myComments.stream()
                    .collect(Collectors.toMap(CommentPo::getId, item -> item, (left, right) -> left));
            List<Long> commentIds = myComments.stream().map(CommentPo::getId).collect(Collectors.toList());
            List<LikeCommentPo> likeCommentPos = likeCommentMapper.selectList(new LambdaQueryWrapper<LikeCommentPo>()
                    .in(LikeCommentPo::getCommentId, commentIds)
                    .ne(LikeCommentPo::getUserId, myId)
                    .orderByDesc(LikeCommentPo::getCreateTime)
                    .orderByDesc(LikeCommentPo::getId));
            if (CollectionUtils.isNotEmpty(likeCommentPos)) {
                Set<Long> userIds = likeCommentPos.stream().map(LikeCommentPo::getUserId).collect(Collectors.toSet());
                Set<Long> videoIds = myComments.stream().map(CommentPo::getVideoId).filter(Objects::nonNull).collect(Collectors.toSet());
                Map<Long, UserPo> userMap = userIds.isEmpty()
                        ? Collections.emptyMap()
                        : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(UserPo::getId, item -> item, (left, right) -> left));
                Map<Long, VideoPo> videoMap = videoIds.isEmpty()
                        ? Collections.emptyMap()
                        : videoMapper.selectBatchIds(videoIds).stream()
                        .collect(Collectors.toMap(VideoPo::getId, item -> item, (left, right) -> left));

                Map<Long, List<LikeCommentPo>> groupedLikeComments = likeCommentPos.stream()
                        .collect(Collectors.groupingBy(LikeCommentPo::getCommentId, LinkedHashMap::new, Collectors.toList()));
                groupedLikeComments.forEach((commentId, likes) -> {
                    CommentPo commentPo = commentMap.get(commentId);
                    if (commentPo == null || CollectionUtils.isEmpty(likes)) {
                        return;
                    }
                    VideoPo videoPo = videoMap.get(commentPo.getVideoId());
                    summaryList.add(LikeReceivedSummaryVo.builder()
                            .targetType("comment")
                            .targetId(commentId)
                            .commentId(commentId)
                            .commentContent(commentPo.getContent())
                            .videoId(commentPo.getVideoId())
                            .videoTitle(videoPo == null ? "" : videoPo.getTitle())
                            .videoCoverUrl(videoPo == null ? null : videoPo.getCoverUrl())
                            .totalCount((long) likes.size())
                            .previewUsernames(extractPreviewUsernames(likes.stream()
                                    .map(LikeCommentPo::getUserId)
                                    .collect(Collectors.toList()), userMap))
                            .latestLikeTime(likes.get(0).getCreateTime())
                            .build());
                });
            }
        }

        List<VideoPo> myVideos = videoMapper.selectList(new LambdaQueryWrapper<VideoPo>()
                .eq(VideoPo::getUserId, myId));
        if (CollectionUtils.isNotEmpty(myVideos)) {
            Map<Long, VideoPo> videoMap = myVideos.stream()
                    .collect(Collectors.toMap(VideoPo::getId, item -> item, (left, right) -> left));
            List<Long> videoIds = myVideos.stream().map(VideoPo::getId).collect(Collectors.toList());
            List<LikeVideoPo> likeVideoPos = likeVideoMapper.selectList(new LambdaQueryWrapper<LikeVideoPo>()
                    .in(LikeVideoPo::getVideoId, videoIds)
                    .ne(LikeVideoPo::getUserId, myId)
                    .orderByDesc(LikeVideoPo::getCreateTime)
                    .orderByDesc(LikeVideoPo::getId));
            if (CollectionUtils.isNotEmpty(likeVideoPos)) {
                Set<Long> userIds = likeVideoPos.stream().map(LikeVideoPo::getUserId).collect(Collectors.toSet());
                Map<Long, UserPo> userMap = userIds.isEmpty()
                        ? Collections.emptyMap()
                        : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(UserPo::getId, item -> item, (left, right) -> left));

                Map<Long, List<LikeVideoPo>> groupedLikeVideos = likeVideoPos.stream()
                        .collect(Collectors.groupingBy(LikeVideoPo::getVideoId, LinkedHashMap::new, Collectors.toList()));
                groupedLikeVideos.forEach((videoId, likes) -> {
                    VideoPo videoPo = videoMap.get(videoId);
                    if (videoPo == null || CollectionUtils.isEmpty(likes)) {
                        return;
                    }
                    summaryList.add(LikeReceivedSummaryVo.builder()
                            .targetType("video")
                            .targetId(videoId)
                            .videoId(videoId)
                            .videoTitle(videoPo.getTitle())
                            .videoCoverUrl(videoPo.getCoverUrl())
                            .totalCount((long) likes.size())
                            .previewUsernames(extractPreviewUsernames(likes.stream()
                                    .map(LikeVideoPo::getUserId)
                                    .collect(Collectors.toList()), userMap))
                            .latestLikeTime(likes.get(0).getCreateTime())
                            .build());
                });
            }
        }

        summaryList.sort((left, right) -> {
            LocalDateTime rightTime = right.getLatestLikeTime();
            LocalDateTime leftTime = left.getLatestLikeTime();
            if (rightTime == null && leftTime == null) {
                return 0;
            }
            if (rightTime == null) {
                return -1;
            }
            if (leftTime == null) {
                return 1;
            }
            return rightTime.compareTo(leftTime);
        });
        return summaryList;
    }

    @Override
    public LikeReceivedDetailVo queryLikeReceivedDetail(String targetType, Long targetId) {
        Long myId = UserHolderUtil.getUser().getUserId();
        if (targetId == null || targetType == null) {
            throw new ErrorParamException("点赞消息参数不完整");
        }

        String normalizedType = targetType.trim().toLowerCase();
        if ("comment".equals(normalizedType)) {
            CommentPo commentPo = commentMapper.selectById(targetId);
            if (commentPo == null || !Objects.equals(commentPo.getUserId(), myId) || !Objects.equals(commentPo.getIsDeleted(), NOT_DELETED)) {
                throw new NotFoundException("评论不存在或无权限查看");
            }
            List<LikeCommentPo> recentLikes = likeCommentMapper.selectList(new LambdaQueryWrapper<LikeCommentPo>()
                    .eq(LikeCommentPo::getCommentId, targetId)
                    .ne(LikeCommentPo::getUserId, myId)
                    .orderByDesc(LikeCommentPo::getCreateTime)
                    .orderByDesc(LikeCommentPo::getId)
                    .last("limit 20"));
            long totalCount = likeCommentMapper.selectCount(new LambdaQueryWrapper<LikeCommentPo>()
                    .eq(LikeCommentPo::getCommentId, targetId)
                    .ne(LikeCommentPo::getUserId, myId));
            Map<Long, UserPo> userMap = queryUserMap(recentLikes.stream()
                    .map(LikeCommentPo::getUserId)
                    .collect(Collectors.toList()));
            VideoPo videoPo = videoMapper.selectById(commentPo.getVideoId());
            return LikeReceivedDetailVo.builder()
                    .targetType("comment")
                    .targetId(targetId)
                    .commentId(targetId)
                    .commentContent(commentPo.getContent())
                    .videoId(commentPo.getVideoId())
                    .videoTitle(videoPo == null ? "" : videoPo.getTitle())
                    .videoCoverUrl(videoPo == null ? null : videoPo.getCoverUrl())
                    .totalCount(totalCount)
                    .recentUsers(recentLikes.stream()
                            .map(item -> buildLikeUserVo(item.getUserId(), item.getCreateTime(), userMap))
                            .collect(Collectors.toList()))
                    .build();
        }

        if ("video".equals(normalizedType)) {
            VideoPo videoPo = videoMapper.selectById(targetId);
            if (videoPo == null || !Objects.equals(videoPo.getUserId(), myId)) {
                throw new NotFoundException("视频不存在或无权限查看");
            }
            List<LikeVideoPo> recentLikes = likeVideoMapper.selectList(new LambdaQueryWrapper<LikeVideoPo>()
                    .eq(LikeVideoPo::getVideoId, targetId)
                    .ne(LikeVideoPo::getUserId, myId)
                    .orderByDesc(LikeVideoPo::getCreateTime)
                    .orderByDesc(LikeVideoPo::getId)
                    .last("limit 20"));
            long totalCount = likeVideoMapper.selectCount(new LambdaQueryWrapper<LikeVideoPo>()
                    .eq(LikeVideoPo::getVideoId, targetId)
                    .ne(LikeVideoPo::getUserId, myId));
            Map<Long, UserPo> userMap = queryUserMap(recentLikes.stream()
                    .map(LikeVideoPo::getUserId)
                    .collect(Collectors.toList()));
            return LikeReceivedDetailVo.builder()
                    .targetType("video")
                    .targetId(targetId)
                    .videoId(targetId)
                    .videoTitle(videoPo.getTitle())
                    .videoCoverUrl(videoPo.getCoverUrl())
                    .totalCount(totalCount)
                    .recentUsers(recentLikes.stream()
                            .map(item -> buildLikeUserVo(item.getUserId(), item.getCreateTime(), userMap))
                            .collect(Collectors.toList()))
                    .build();
        }

        throw new ErrorParamException("点赞消息类型非法");
    }

    @Override
    public List<UserSimpleInfoVo> queryFansList(Long userId) {
        Long currentUserId = UserHolderUtil.getUser().getUserId();
        if (!Objects.equals(currentUserId, userId) && hasMutualBlock(currentUserId, userId)) {
            throw new ErrorOperationException("由于隐私设置，无法查看该用户粉丝列表");
        }
        // 查询粉丝列表
        List<FollowRecordPo> fans = followRecordMapper.selectList(new QueryWrapper<FollowRecordPo>().lambda()
                .eq(FollowRecordPo::getFolloweeId, userId)
                .eq(FollowRecordPo::getIsDeleted, NOT_DELETED)
                .orderByDesc(FollowRecordPo::getCreateTime)
                .orderByDesc(FollowRecordPo::getId));
        if (CollectionUtils.isEmpty(fans)) {
            return Collections.emptyList();
        }
        List<Long> fanIds = fans.stream()
                .map(FollowRecordPo::getFollowerId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(fanIds)) {
            return Collections.emptyList();
        }

        Set<Long> followedFanIds = queryFolloweeIdSet(currentUserId, fanIds);
        return buildRelationUserList(fanIds, followedFanIds, currentUserId);
    }

    @Override
    public List<UserSimpleInfoVo> queryFollowList(Long userId) {
        Long currentUserId = UserHolderUtil.getUser().getUserId();
        if (!Objects.equals(currentUserId, userId) && hasMutualBlock(currentUserId, userId)) {
            throw new ErrorOperationException("由于隐私设置，无法查看该用户关注列表");
        }
        // 查询关注列表
        List<FollowRecordPo> follows = followRecordMapper.selectList(new QueryWrapper<FollowRecordPo>().lambda()
                .eq(FollowRecordPo::getFollowerId, userId)
                .eq(FollowRecordPo::getIsDeleted, NOT_DELETED)
                .orderByDesc(FollowRecordPo::getCreateTime)
                .orderByDesc(FollowRecordPo::getId));
        if (CollectionUtils.isEmpty(follows)) {
            return Collections.emptyList();
        }
        List<Long> followsIdList = follows.stream()
                .map(FollowRecordPo::getFolloweeId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(followsIdList)) {
            return Collections.emptyList();
        }

        Set<Long> followedUserIds = queryFolloweeIdSet(currentUserId, followsIdList);
        return buildRelationUserList(followsIdList, followedUserIds, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void collectVideo(CollectVideoRequest request) {
        Long userId = UserHolderUtil.getUser().getUserId();
        if (request == null || request.getVideoId() == null) {
            throw new ErrorParamException("收藏参数不完整");
        }
        Long videoId = request.getVideoId();
        List<Long> collectDirectoryIdList = request.getCollectDirectoryIdList();
        List<Long> removeDirectoryIdList = request.getRemoveDirectoryIdList();

        VideoPo videoPo = videoMapper.selectById(videoId);
        if (videoPo == null || videoPo.getStatus() != VideoStatusEnum.PUBLISHED.getCode()) {
            throw new NotFoundException("视频不存在或不可收藏");
        }

        List<Long> directoryIdList = Optional
                .ofNullable(interactRepository.batchQueryUserCollectionDirectory(userId, null))
                .orElse(Collections.emptyList()).stream()
                .map(CollectionDirectoryPo::getId)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(directoryIdList)) {
            return;
        }

        // 操作前是否收藏该视频
        boolean beforeCollect = interactRelationService.isCollectVideo(userId, videoId);

        // --- 执行收藏 ---
        for (Long directoryId : CollectionUtils.emptyIfNull(collectDirectoryIdList)) {
            // 如果有收藏记录
            List<CollectionItemPo> collectionItemPos = interactRepository.queryUserCollectRecord(userId, videoId, directoryId);
            if (CollectionUtils.isNotEmpty(collectionItemPos)) {
                CollectionItemPo itemPo = collectionItemPos.get(0);
                if (itemPo.getIsDeleted().equals(NOT_DELETED)) {
                    throw new ErrorOperationException("该收藏夹已收藏该视频");
                } else {
                    itemPo.setIsDeleted(NOT_DELETED);
                    collectionItemMapper.updateById(itemPo);
                    continue;
                }
            }
            // 如果没有收藏记录
            try {
                collectionItemMapper.insert(CollectionItemPo.builder()
                        .directoryId(directoryId).userId(userId).videoId(videoId).build());
            } catch (DuplicateKeyException e) {
                // 幂等处理：如果已经收藏过，直接忽略，不重复加分
                log.warn("用户重复收藏，用户ID：{}，视频ID：{}", userId, videoId);
            }
        }

        // --- 执行取消收藏 ---
        for (Long directoryId : CollectionUtils.emptyIfNull(removeDirectoryIdList)) {
            // 如果有收藏记录
            List<CollectionItemPo> collectionItemPos = interactRepository.queryUserCollectRecord(userId, videoId, directoryId);
            if (CollectionUtils.isNotEmpty(collectionItemPos)) {
                CollectionItemPo itemPo = collectionItemPos.get(0);
                if (itemPo.getIsDeleted().equals(NOT_DELETED)) {
                    itemPo.setIsDeleted(DELETED);
                    collectionItemMapper.updateById(itemPo);
                }
            }
            // 如果没有收藏记录则直接跳过
        }

        // 操作后是否收藏该视频
        boolean afterCollect = interactRelationService.isCollectVideo(userId, videoId);
        if (beforeCollect && !afterCollect) {
            videoStatsMapper.updateCollectCount(videoId, -1);
            esSyncService.syncVideo(videoId);
        } else if (!beforeCollect && afterCollect) {
            videoStatsMapper.updateCollectCount(videoId, 1);
            esSyncService.syncVideo(videoId);
        }
    }

    @Override
    public void submitReport(ReportSubmitRequest request) {
        Long reporterId = UserHolderUtil.getUser().getUserId();
        if (request == null || request.getTargetType() == null || request.getTargetId() == null) {
            throw new ErrorParamException("举报参数不完整");
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new ErrorParamException("举报原因不能为空");
        }

        Integer targetType = request.getTargetType();
        Long targetId = request.getTargetId();
        if (ReportTargetTypeEnum.USER.getCode().equals(targetType)) {
            if (Objects.equals(reporterId, targetId)) {
                throw new ErrorOperationException("不能举报自己");
            }
            UserPo targetUser = userMapper.selectById(targetId);
            if (targetUser == null) {
                throw new NotFoundException("被举报用户不存在");
            }
        } else if (ReportTargetTypeEnum.VIDEO.getCode().equals(targetType)) {
            VideoPo targetVideo = videoMapper.selectById(targetId);
            if (targetVideo == null) {
                throw new NotFoundException("被举报视频不存在");
            }
        } else if (ReportTargetTypeEnum.COMMENT.getCode().equals(targetType)) {
            CommentPo targetComment = commentMapper.selectById(targetId);
            if (targetComment == null || !NOT_DELETED.equals(targetComment.getIsDeleted())) {
                throw new NotFoundException("被举报评论不存在");
            }
        } else {
            throw new ErrorParamException("举报类型非法");
        }

        ReportPo reportPo = ReportPo.builder()
                .reporterId(reporterId)
                .reportType(targetType)
                .targetId(targetId)
                .reason(request.getReason())
                .detail(request.getDetail())
                .status(ReportStatusEnum.WAITING_AUDIT.getCode())
                .build();
        reportMapper.insert(reportPo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCollectionDirectory(CollectionDirectoryCreateRequest request) {
        Long myId = UserHolderUtil.getUser().getUserId();
        if (request == null || request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ErrorParamException("收藏夹名称不能为空");
        }
        CollectionDirectoryPo po = CollectionDirectoryPo.builder()
                .userId(myId)
                .name(request.getName().trim())
                .description(request.getDescription())
                .coverUrl(request.getCoverUrl())
                .isPublic(request.getIsPublic() != null && request.getIsPublic() == OPERATION_ON ? 1 : 0)
                .isDefault(0)
                .isDeleted(NOT_DELETED)
                .build();
        collectionDirectoryMapper.insert(po);
        return po.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCollectionDirectory(CollectionDirectoryUpdateRequest request) {
        Long myId = UserHolderUtil.getUser().getUserId();
        if (request == null || request.getDirectoryId() == null) {
            throw new ErrorParamException("收藏夹参数不完整");
        }
        CollectionDirectoryPo po = queryOwnedDirectory(myId, request.getDirectoryId());
        if (po == null) {
            throw new NotFoundException("收藏夹不存在或无权限");
        }
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            po.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            po.setDescription(request.getDescription());
        }
        if (request.getCoverUrl() != null) {
            po.setCoverUrl(request.getCoverUrl());
        }
        if (request.getIsPublic() != null) {
            po.setIsPublic(request.getIsPublic() == OPERATION_ON ? 1 : 0);
        }
        collectionDirectoryMapper.updateById(po);
    }

    @Override
    public List<CollectionDirectoryVo> listCollectionDirectories(Long targetUserId) {
        Long myId = UserHolderUtil.getUser().getUserId();
        Long ownerId = targetUserId == null ? myId : targetUserId;
        if (!Objects.equals(myId, ownerId) && hasMutualBlock(myId, ownerId)) {
            throw new ErrorOperationException("由于隐私设置，无法查看收藏夹");
        }
        LambdaQueryWrapper<CollectionDirectoryPo> wrapper = new LambdaQueryWrapper<CollectionDirectoryPo>()
                .eq(CollectionDirectoryPo::getUserId, ownerId)
                .eq(CollectionDirectoryPo::getIsDeleted, NOT_DELETED)
                .orderByDesc(CollectionDirectoryPo::getIsDefault)
                .orderByDesc(CollectionDirectoryPo::getId);
        if (!Objects.equals(myId, ownerId)) {
            wrapper.eq(CollectionDirectoryPo::getIsPublic, 1);
        }
        List<CollectionDirectoryPo> list = collectionDirectoryMapper.selectList(wrapper);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<CollectionDirectoryVo> result = new ArrayList<>();
        for (CollectionDirectoryPo po : list) {
            Long itemCount = collectionItemMapper.countByDirectory(po.getId());
            result.add(CollectionDirectoryVo.builder()
                    .directoryId(po.getId())
                    .name(po.getName())
                    .description(po.getDescription())
                    .coverUrl(po.getCoverUrl())
                    .isPublic(po.getIsPublic())
                    .isDefault(Objects.equals(po.getIsDefault(), 1))
                    .itemCount(itemCount == null ? 0L : itemCount)
                    .build());
        }
        return result;
    }

    /**
     * 查询某视频在所有收藏夹的状态
     */
    @Override
    public List<VideoDirectoryRelationVo> queryVideoDirectoryRelations(Long videoId) {
        Long userId = UserHolderUtil.getUser().getUserId();
        if (videoId == null) {
            throw new ErrorParamException("视频ID不能为空");
        }
        List<CollectionDirectoryPo> directories = interactRepository.queryUserCollectionDirectory(userId);
        List<CollectionItemPo> items = interactRepository.queryUserCollectionItemWithVideo(userId, videoId);
        Set<Long> directoryIds = Optional.ofNullable(items).orElse(Collections.emptyList()).stream()
                .map(CollectionItemPo::getDirectoryId).collect(Collectors.toSet());

        List<VideoDirectoryRelationVo> result = new ArrayList<>();
        for (CollectionDirectoryPo directoryPo : directories) {
            VideoDirectoryRelationVo vo = VideoDirectoryRelationVo.builder()
                    .videoId(videoId)
                    .directoryId(directoryPo.getId())
                    .directoryName(directoryPo.getName())
                    .isPublic(directoryPo.getIsPublic() == 1)
                    .isDefault(directoryPo.getIsDefault() == 1)
                    .isCollect(false)
                    .build();
            if (directoryIds.contains(directoryPo.getId())) {
                vo.setIsCollect(true);
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer batchOperateCollectionItems(CollectionBatchOperateRequest request) {
        Long userId = UserHolderUtil.getUser().getUserId();
        if (request == null || request.getSourceDirectoryId() == null || request.getOperation() == null
                || request.getTargetDirectoryId() == null || CollectionUtils.isEmpty(request.getVideoIds())) {
            throw new ErrorParamException("批量操作参数不完整");
        }
        CollectionDirectoryPo sourceDirectory = queryOwnedDirectory(userId, request.getSourceDirectoryId());
        if (sourceDirectory == null) {
            throw new NotFoundException("来源收藏夹不存在或无权限");
        }
        CollectionDirectoryPo targetDirectory = queryOwnedDirectory(userId, request.getTargetDirectoryId());
        if (targetDirectory == null) {
            throw new NotFoundException("目标收藏夹不存在或无权限");
        }

        Integer operation = request.getOperation();
        List<Long> videoIdList = request.getVideoIds();

        // 批量取消收藏
        if (operation.equals(BatchCollectOperationEnum.CANCEL_COLLECT.getCode())) {
            int canceled1 = interactRepository.batchCollectVideo(userId, sourceDirectory.getId(), videoIdList);
            int canceled2 = videoRepository.batchAddVideoCollectCount(videoIdList, -1L);
            if (canceled1 != videoIdList.size() || canceled2 != videoIdList.size()) {
                throw new ErrorOperationException("批量操作失败");
            }
            return canceled1;
        }

        // 批量复制
        if (operation.equals(BatchCollectOperationEnum.COPY.getCode())) {
            int added = interactRepository.batchCollectVideo(userId, targetDirectory.getId(), videoIdList);
            if (added != videoIdList.size()) {
                throw new ErrorOperationException("批量操作失败");
            }
            return added;
        }

        // 批量移动
        if (operation.equals(BatchCollectOperationEnum.MOVE.getCode())) {
            int canceled = interactRepository.batchCancelCollectVideo(userId, sourceDirectory.getId(), videoIdList);
            int added = interactRepository.batchCollectVideo(userId, targetDirectory.getId(), videoIdList);
            if (canceled != added) {
                throw new ErrorOperationException("批量操作失败");
            }
            return canceled;
        }

        throw new ErrorParamException("批量操作参数错误");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer clearInvalidCollectionItems(Long directoryId) {
        Long myId = UserHolderUtil.getUser().getUserId();
        if (directoryId == null) {
            throw new ErrorParamException("收藏夹ID不能为空");
        }
        CollectionDirectoryPo directoryPo = queryOwnedDirectory(myId, directoryId);
        if (directoryPo == null) {
            throw new NotFoundException("收藏夹不存在或无权限");
        }

        LambdaQueryWrapper<CollectionItemPo> wrapper = new LambdaQueryWrapper<CollectionItemPo>()
                .eq(CollectionItemPo::getUserId, myId)
                .eq(CollectionItemPo::getDirectoryId, directoryId)
                .eq(CollectionItemPo::getIsDeleted, NOT_DELETED);
        List<CollectionItemPo> items = collectionItemMapper.selectList(wrapper);
        if (items == null || items.isEmpty()) {
            return 0;
        }
        int affected = 0;
        for (CollectionItemPo item : items) {
            VideoPo videoPo = videoMapper.selectById(item.getVideoId());
            boolean invalid = videoPo == null || videoPo.getStatus() == VideoStatusEnum.DELETED.getCode()
                    || videoPo.getStatus() == VideoStatusEnum.BANNED.getCode();
            if (invalid) {
                item.setIsDeleted(1);
                collectionItemMapper.updateById(item);
                videoStatsMapper.updateCollectCount(item.getVideoId(), -1);
                affected++;
            }
        }
        return affected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void coinVideo(CoinVideoRequest request) {
        Long myId = UserHolderUtil.getUser().getUserId();
        if (request == null || request.getVideoId() == null || request.getAmount() == null) {
            throw new ErrorParamException("投币参数不完整");
        }
        if (request.getAmount() != 1 && request.getAmount() != 2) {
            throw new ErrorParamException("投币数量只能为1或2");
        }
        VideoPo videoPo = videoMapper.selectById(request.getVideoId());
        if (videoPo == null || videoPo.getStatus() != VideoStatusEnum.PUBLISHED.getCode()) {
            throw new NotFoundException("视频不存在或不可投币");
        }

        ensureWalletRow(myId);
        Integer oldAmount = userCoinChangeLogMapper.sumConsumedCoinByTarget(myId, COIN_CHANGE_TYPE_VIDEO_REWARD, request.getVideoId());
        if (oldAmount == null) {
            oldAmount = 0;
        }
        int targetAmount = oldAmount + request.getAmount();
        if (targetAmount > 2) {
            throw new ErrorOperationException("单个视频最多投币2个");
        }

        int deducted = userWalletMapper.deductCoinIfEnough(myId, request.getAmount().longValue());
        if (deducted <= 0) {
            throw new ErrorOperationException("硬币余额不足");
        }

        recordCoinChangeLog(myId, -request.getAmount(), COIN_CHANGE_TYPE_VIDEO_REWARD, request.getVideoId());
        videoStatsMapper.updateCoinCount(request.getVideoId(), request.getAmount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean claimDailyCoin(Long userId) {
        if (userId == null) {
            return false;
        }
        ensureWalletRow(userId);
        LocalDateTime startTime = LocalDate.now().atStartOfDay();
        LocalDateTime endTime = startTime.plusDays(1);
        Integer existsCount = userCoinChangeLogMapper.countLogsInRange(
                userId,
                COIN_CHANGE_TYPE_DAILY_REWARD,
                startTime,
                endTime
        );
        if (existsCount != null && existsCount > 0) {
            return false;
        }
        userWalletMapper.updateCoinBalance(userId, (long) DAILY_LOGIN_REWARD);
        recordCoinChangeLog(userId, DAILY_LOGIN_REWARD, COIN_CHANGE_TYPE_DAILY_REWARD, null);
        return true;
    }

    @Override
    public CoinWalletVo queryMyWallet() {
        Long myId = UserHolderUtil.getUser().getUserId();
        ensureWalletRow(myId);
        UserWalletPo walletPo = userWalletMapper.selectOne(new LambdaQueryWrapper<UserWalletPo>()
                .eq(UserWalletPo::getUserId, myId)
                .last("limit 1"));
        return CoinWalletVo.builder()
                .userId(myId)
                .balance(walletPo == null ? 0L : walletPo.getCoinBalance())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void tripleAction(TripleActionRequest request) {
        if (request == null || request.getVideoId() == null) {
            throw new ErrorParamException("一键三连参数不完整");
        }
        likeVideo(LikeRequest.builder().targetId(request.getVideoId()).operation(1).build());
        coinVideo(CoinVideoRequest.builder().videoId(request.getVideoId()).amount(1).build());
        Long myId = UserHolderUtil.getUser().getUserId();
        CollectionDirectoryPo defaultDirectory = getOrCreateDefaultDirectory(myId);
        collectVideo(CollectVideoRequest.builder()
                .videoId(request.getVideoId())
                .collectDirectoryIdList(Collections.singletonList(defaultDirectory.getId()))
                .build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean shareVideo(Long videoId) {
        Long userId = UserHolderUtil.getUser().getUserId();
        if (videoId == null) {
            throw new ErrorParamException("视频ID不能为空");
        }
        VideoPo videoPo = videoMapper.selectById(videoId);
        if (videoPo == null || !Objects.equals(videoPo.getStatus(), VideoStatusEnum.PUBLISHED.getCode())) {
            throw new NotFoundException("视频不存在或不可分享");
        }

        String shareDedupKey = RedisKeyUtil.videoShareDedupKey(videoId, userId);
        if (stringRedisTemplate.hasKey(shareDedupKey)) {
            return false;
        }

        VideoShareRecordPo shareRecordPo = videoShareRecordMapper.selectOne(new LambdaQueryWrapper<VideoShareRecordPo>()
                .eq(VideoShareRecordPo::getVideoId, videoId)
                .eq(VideoShareRecordPo::getUserId, userId)
                .last("limit 1"));
        if (shareRecordPo != null) {
            rebuildVideoShareDedupCache(shareDedupKey);
            return false;
        }

        try {
            videoShareRecordMapper.insert(VideoShareRecordPo.builder()
                    .videoId(videoId)
                    .userId(userId)
                    .build());
            int updated = videoStatsMapper.updateShareCount(videoId, 1);
            if (updated <= 0) {
                throw new ErrorOperationException("分享计数更新失败");
            }
            rebuildVideoShareDedupCache(shareDedupKey);
            return true;
        } catch (DuplicateKeyException e) {
            rebuildVideoShareDedupCache(shareDedupKey);
            return false;
        }
    }

    private void rebuildVideoShareDedupCache(String shareDedupKey) {
        stringRedisTemplate.opsForValue().set(shareDedupKey, "1", VIDEO_SHARE_DEDUP_TTL);
    }

    private void cancelFollowIfPresent(Long followerId, Long followeeId) {
        FollowRecordPo followRecordPo = followRecordMapper.selectOne(new LambdaQueryWrapper<FollowRecordPo>()
                .eq(FollowRecordPo::getFollowerId, followerId)
                .eq(FollowRecordPo::getFolloweeId, followeeId)
                .eq(FollowRecordPo::getIsDeleted, NOT_DELETED)
                .last("limit 1"));
        if (followRecordPo == null) {
            return;
        }
        followRecordPo.setIsDeleted(1);
        followRecordMapper.updateById(followRecordPo);
        interactRepository.updateUserFollowNum(followerId, -1L);
        interactRepository.updateUserFansNum(followeeId, -1L);
        esSyncService.syncUser(followeeId);
    }

    private CollectionDirectoryPo queryOwnedDirectory(Long userId, Long directoryId) {
        return collectionDirectoryMapper.selectOne(new LambdaQueryWrapper<CollectionDirectoryPo>()
                .eq(CollectionDirectoryPo::getId, directoryId)
                .eq(CollectionDirectoryPo::getUserId, userId)
                .eq(CollectionDirectoryPo::getIsDeleted, NOT_DELETED)
                .last("limit 1"));
    }

    private CollectionDirectoryPo getOrCreateDefaultDirectory(Long userId) {
        CollectionDirectoryPo defaultDirectory = collectionDirectoryMapper.selectDefaultDirectory(userId);
        if (defaultDirectory != null) {
            return defaultDirectory;
        }
        CollectionDirectoryPo po = CollectionDirectoryPo.builder()
                .userId(userId)
                .name(DEFAULT_COLLECTION_NAME)
                .description("系统默认收藏夹")
                .isPublic(0)
                .isDefault(1)
                .isDeleted(NOT_DELETED)
                .build();
        collectionDirectoryMapper.insert(po);
        return po;
    }

    private void ensureWalletRow(Long userId) {
        UserWalletPo walletPo = userWalletMapper.selectOne(new LambdaQueryWrapper<UserWalletPo>()
                .eq(UserWalletPo::getUserId, userId)
                .last("limit 1"));
        if (walletPo != null) {
            return;
        }
        try {
            userWalletMapper.insert(UserWalletPo.builder()
                    .userId(userId)
                    .coinBalance(0L)
                    .build());
        } catch (DuplicateKeyException ignore) {
        }
    }

    private void recordCoinChangeLog(Long userId, Integer changeAmount, Integer changeType, Long relatedTargetId) {
        userCoinChangeLogMapper.insert(UserCoinChangeLogPo.builder()
                .userId(userId)
                .changeAmount(changeAmount)
                .changeType(changeType)
                .relatedTargetId(relatedTargetId)
                .build());
    }

    /**
     * 供定时任务调用：关闭超时会话并清理映射
     */
    public void closeIdleChatSessions(long idleMillis) {
        long now = System.currentTimeMillis();
        for (Map.Entry<Long, Set<WebSocketSession>> entry : chatSessions.entrySet()) {
            Long userId = entry.getKey();
            Set<WebSocketSession> sessions = entry.getValue();
            if (sessions == null || sessions.isEmpty()) {
                continue;
            }
            for (WebSocketSession s : sessions) {
                if (s == null) {
                    continue;
                }
                Long last = sessionLastSeen.get(s.getId());
                if (last != null && (now - last) > idleMillis) {
                    try {
                        s.close(CloseStatus.SESSION_NOT_RELIABLE.withReason("心跳超时"));
                    } catch (Exception ignore) {
                    } finally {
                        removeChatSession(userId, s);
                    }
                }
            }
        }
    }

    /**
     * 查询当前用户的举报信息
     */
    @Override
    public List<ManagerReportRecordVo> listMyReports() {
        Long myId = UserHolderUtil.getUser().getUserId();
        LambdaQueryWrapper<ReportPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportPo::getReporterId, myId)
                .orderByDesc(ReportPo::getCreateTime);
        List<ReportPo> reportList = reportMapper.selectList(wrapper);
        if (reportList == null || reportList.isEmpty()) {
            return Collections.emptyList();
        }
        return reportList.stream().map(po -> ManagerReportRecordVo.builder()
                .reportId(po.getId())
                .reportType(po.getReportType())
                .reason(po.getReason())
                .detail(po.getDetail())
                .status(po.getStatus())
                .reviewNote(po.getReviewNote())
                .createTime(po.getCreateTime())
                .updateTime(po.getUpdateTime())
                .build()).collect(Collectors.toList());
    }

    @Override
    public List<SearchVideoVo> listCollectionItems(Long directoryId, Integer sortType) {
        if (directoryId == null) {
            return Collections.emptyList();
        }

        Long currentUserId = UserHolderUtil.getUser().getUserId();
        CollectionDirectoryPo directoryPo = queryVisibleDirectory(currentUserId, directoryId);
        if (directoryPo == null) {
            throw new NotFoundException("收藏夹不存在或无权限查看");
        }

        List<CollectionItemPo> items = collectionItemMapper.selectList(new LambdaQueryWrapper<CollectionItemPo>().eq(CollectionItemPo::getDirectoryId, directoryId).eq(CollectionItemPo::getIsDeleted, NOT_DELETED).orderByDesc(CollectionItemPo::getCreateTime));

        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> videoIds = items.stream().map(CollectionItemPo::getVideoId).filter(Objects::nonNull).distinct().collect(Collectors.toList());

        if (videoIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, SearchVideoVo> videoVoMap = searchService.queryVideoVoMapFromEs(videoIds);
        List<SearchVideoVo> result = items.stream()
                .map(item -> mergeCollectionItemVideo(item, videoVoMap.get(item.getVideoId())))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 根据排序类型排序
        if (sortType != null) {
            switch (sortType) {
                case 1: // 最近收藏
                    result.sort((a, b) -> {
                        if (a.getCollectTime() == null) return 1;
                        if (b.getCollectTime() == null) return -1;
                        return b.getCollectTime().compareTo(a.getCollectTime());
                    });
                    break;
                case 2: // 最多播放
                    result.sort((a, b) -> Long.compare(b.getPlayCount() != null ? b.getPlayCount() : 0, a.getPlayCount() != null ? a.getPlayCount() : 0));
                    break;
                case 3: // 最近投稿
                    result.sort((a, b) -> {
                        if (a.getCreateTime() == null) return 1;
                        if (b.getCreateTime() == null) return -1;
                        return b.getCreateTime().compareTo(a.getCreateTime());
                    });
                    break;
            }
        }

        return result;
    }

    @Override
    public void deleteCollectionDirectory(Long directoryId) {
        Long myId = UserHolderUtil.getUser().getUserId();
        if (directoryId == null) {
            throw new ErrorParamException("收藏夹ID不能为空");
        }
        CollectionDirectoryPo directoryPo = queryOwnedDirectory(myId, directoryId);
        if (directoryPo == null) {
            throw new NotFoundException("收藏夹不存在或无权限");
        }
        if (Objects.equals(directoryPo.getIsDefault(), 1)) {
            throw new ErrorOperationException("默认收藏夹无法删除");
        }
        directoryPo.setIsDeleted(1);
        collectionDirectoryMapper.updateById(directoryPo);

        // 同步更新视频统计数据
        List<CollectionItemPo> items = collectionItemMapper.selectList(new LambdaQueryWrapper<CollectionItemPo>()
                .eq(CollectionItemPo::getDirectoryId, directoryId)
                .eq(CollectionItemPo::getIsDeleted, NOT_DELETED));
        if (items != null && !items.isEmpty()) {
            for (CollectionItemPo item : items) {
                item.setIsDeleted(1);
                collectionItemMapper.updateById(item);
                videoStatsMapper.updateCollectCount(item.getVideoId(), -1);
                esSyncService.syncVideo(item.getVideoId());
            }
        }
    }

    private List<UserSimpleInfoVo> buildRelationUserList(List<Long> orderedUserIds, Set<Long> followedUserIds, Long currentUserId) {
        if (CollectionUtils.isEmpty(orderedUserIds)) {
            return Collections.emptyList();
        }
        Set<Long> fanUserIds = queryFollowerIdSet(currentUserId, orderedUserIds);
        Map<Long, SearchUserVo> userVoMap = searchService.queryUserVoMapFromEs(orderedUserIds);
        List<UserSimpleInfoVo> result = new ArrayList<>();
        for (Long targetUserId : orderedUserIds) {
            if (targetUserId == null || hasMutualBlock(currentUserId, targetUserId)) {
                continue;
            }
            SearchUserVo userVo = userVoMap.get(targetUserId);
            if (userVo == null) {
                continue;
            }
            result.add(UserSimpleInfoVo.builder()
                    .userId(userVo.getUserId())
                    .username(userVo.getUsername())
                    .avatarUrl(userVo.getAvatar())
                    .bio(userVo.getBio())
                    .isFollow(followedUserIds.contains(targetUserId))
                    .isFans(fanUserIds.contains(targetUserId))
                    .build());
        }
        return result;
    }

    private SearchVideoVo mergeCollectionItemVideo(CollectionItemPo item, SearchVideoVo videoVo) {
        if (item == null || videoVo == null) {
            return null;
        }
        return SearchVideoVo.builder()
                .videoId(videoVo.getVideoId())
                .title(videoVo.getTitle())
                .userId(videoVo.getUserId())
                .username(videoVo.getUsername())
                .coverUrl(videoVo.getCoverUrl())
                .playCount(videoVo.getPlayCount())
                .createTime(videoVo.getCreateTime())
                .collectionCount(videoVo.getCollectionCount())
                .duration(videoVo.getDuration())
                .collectTime(item.getCreateTime())
                .build();
    }

    private CollectionDirectoryPo queryVisibleDirectory(Long currentUserId, Long directoryId) {
        if (directoryId == null) {
            return null;
        }
        CollectionDirectoryPo directoryPo = collectionDirectoryMapper.selectById(directoryId);
        if (directoryPo == null || !Objects.equals(directoryPo.getIsDeleted(), NOT_DELETED)) {
            return null;
        }
        if (Objects.equals(currentUserId, directoryPo.getUserId())) {
            return directoryPo;
        }
        if (hasMutualBlock(currentUserId, directoryPo.getUserId())) {
            return null;
        }
        return Objects.equals(directoryPo.getIsPublic(), 1) ? directoryPo : null;
    }

    private Set<Long> queryFolloweeIdSet(Long followerId, List<Long> followeeIds) {
        if (followerId == null || CollectionUtils.isEmpty(followeeIds)) {
            return Collections.emptySet();
        }
        List<FollowRecordPo> followRecordPos = followRecordMapper.selectList(new QueryWrapper<FollowRecordPo>().lambda()
                .eq(FollowRecordPo::getFollowerId, followerId)
                .in(FollowRecordPo::getFolloweeId, followeeIds)
                .eq(FollowRecordPo::getIsDeleted, NOT_DELETED));
        return followRecordPos.stream()
                .map(FollowRecordPo::getFolloweeId)
                .collect(Collectors.toSet());
    }

    private Set<Long> queryFollowerIdSet(Long followeeId, List<Long> followerIds) {
        if (followeeId == null || CollectionUtils.isEmpty(followerIds)) {
            return Collections.emptySet();
        }
        List<FollowRecordPo> followRecordPos = followRecordMapper.selectList(new QueryWrapper<FollowRecordPo>().lambda()
                .eq(FollowRecordPo::getFolloweeId, followeeId)
                .in(FollowRecordPo::getFollowerId, followerIds)
                .eq(FollowRecordPo::getIsDeleted, NOT_DELETED));
        return followRecordPos.stream()
                .map(FollowRecordPo::getFollowerId)
                .collect(Collectors.toSet());
    }

    private List<String> extractPreviewUsernames(List<Long> userIds, Map<Long, UserPo> userMap) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        List<String> usernames = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        for (Long userId : userIds) {
            if (userId == null || !visited.add(userId)) {
                continue;
            }
            UserPo userPo = userMap.get(userId);
            usernames.add(userPo == null ? "未知用户" : userPo.getUsername());
            if (usernames.size() >= 2) {
                break;
            }
        }
        return usernames;
    }

    private LikeUserVo buildLikeUserVo(Long userId, LocalDateTime likeTime, Map<Long, UserPo> userMap) {
        UserPo userPo = userMap.get(userId);
        return LikeUserVo.builder()
                .userId(userId)
                .username(userPo == null ? "未知用户" : userPo.getUsername())
                .avatarUrl(userPo == null ? null : userPo.getAvatarUrl())
                .likeTime(likeTime)
                .build();
    }

    private Map<Long, UserPo> queryUserMap(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        List<UserPo> userPos = userMapper.selectList(new QueryWrapper<UserPo>().lambda()
                .in(UserPo::getId, userIds));
        return userPos.stream()
                .collect(Collectors.toMap(UserPo::getId, item -> item, (left, right) -> left));
    }

    @Override
    public CommentPageVo listVideoComments(CommentListRequest request) {
        Long videoId = request.getVideoId();
        if (videoId == null) {
            throw new ErrorParamException("视频ID不能为空");
        }
        int safePageSize = request.getPageSize() == null ? 20 : Math.min(Math.max(request.getPageSize(), 1), 50);
        VideoPo videoPo = videoMapper.selectById(videoId);
        if (videoPo == null || !Objects.equals(videoPo.getStatus(), VideoStatusEnum.PUBLISHED.getCode())) {
            throw new NotFoundException("视频不存在或未发布");
        }

        Long myId = getCurrentUserIdOrNull();
        if (Objects.equals(request.getSortType(), 1)) {
            return listHotComments(videoId, myId, request, safePageSize);
        }
        return listLatestComments(videoId, myId, request, safePageSize);
    }

    private CommentPageVo listLatestComments(Long videoId, Long currentUserId, CommentListRequest request, Integer pageSize) {
        LambdaQueryWrapper<CommentPo> queryWrapper = new LambdaQueryWrapper<CommentPo>()
                .eq(CommentPo::getVideoId, videoId)
                .eq(CommentPo::getRootId, 0L)
                .eq(CommentPo::getIsDeleted, NOT_DELETED);
        if (request.getLastCreateTime() != null && request.getLastCommentId() != null) {
            queryWrapper.eq(CommentPo::getIsTop, 0);
            queryWrapper.and(wrapper -> wrapper
                    .lt(CommentPo::getCreateTime, request.getLastCreateTime())
                    .or(inner -> inner
                            .eq(CommentPo::getCreateTime, request.getLastCreateTime())
                            .lt(CommentPo::getId, request.getLastCommentId())));
        }
        queryWrapper.orderByDesc(CommentPo::getIsTop)
                .orderByDesc(CommentPo::getCreateTime)
                .orderByDesc(CommentPo::getId)
                .last("LIMIT " + (pageSize + 1));

        List<CommentPo> comments = commentMapper.selectList(queryWrapper);
        Long total = interactRepository.queryTotalRootCommentNum(videoId);
        return buildLatestCommentPage(comments, total, pageSize, currentUserId);
    }

    @Override
    public PageVo<CommentVo> listCommentReplies(Long rootCommentId, Integer pageNum, Integer pageSize) {
        if (rootCommentId == null) {
            throw new ErrorParamException("主评论ID不能为空");
        }
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null ? 10 : Math.min(Math.max(pageSize, 1), 50);
        CommentPo rootComment = commentMapper.selectById(rootCommentId);
        if (rootComment == null || !Objects.equals(rootComment.getIsDeleted(), NOT_DELETED) || !Objects.equals(rootComment.getRootId(), 0L)) {
            throw new NotFoundException("主评论不存在");
        }

        Long myId = getCurrentUserIdOrNull();
        if (CollectionUtils.isEmpty(filterBlockedComments(Collections.singletonList(rootComment), myId))) {
            return PageVo.<CommentVo>builder()
                    .total(0L)
                    .pageNum(safePageNum)
                    .pageSize(safePageSize)
                    .isEnd(true)
                    .records(Collections.emptyList())
                    .build();
        }
        List<CommentPo> replyComments = commentMapper.selectList(new LambdaQueryWrapper<CommentPo>()
                .eq(CommentPo::getRootId, rootCommentId)
                .eq(CommentPo::getIsDeleted, NOT_DELETED)
                .orderByAsc(CommentPo::getCreateTime)
                .orderByAsc(CommentPo::getId));
        replyComments = filterBlockedComments(replyComments, myId);

        if (CollectionUtils.isEmpty(replyComments)) {
            return PageVo.<CommentVo>builder()
                    .total(0L)
                    .pageNum(safePageNum)
                    .pageSize(safePageSize)
                    .isEnd(true)
                    .records(Collections.emptyList())
                    .build();
        }

        int fromIndex = Math.min((safePageNum - 1) * safePageSize, replyComments.size());
        int toIndex = Math.min(fromIndex + safePageSize, replyComments.size());
        List<CommentPo> pageRecords = fromIndex >= toIndex
                ? Collections.emptyList()
                : new ArrayList<>(replyComments.subList(fromIndex, toIndex));
        PageVo<CommentVo> pageVo = buildCommentPage(pageRecords, (long) replyComments.size(), safePageNum, safePageSize, myId);
        pageVo.setIsEnd(toIndex >= replyComments.size());
        return pageVo;
    }

    @Override
    public PageVo<CreatorCommentManageVo> listCreatorComments(CreatorCommentQueryRequest request) {
        Long userId = UserHolderUtil.getUser().getUserId();
        int safePageNum = request == null || request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
        int safePageSize = request == null || request.getPageSize() == null ? 12 : Math.min(Math.max(request.getPageSize(), 1), 50);
        long offset = (long) (safePageNum - 1) * safePageSize;
        String keyword = request == null ? null : request.getKeyword();
        Long videoId = request == null ? null : request.getVideoId();
        Integer sortType = request == null ? null : request.getSortType();

        if (videoId != null) {
            VideoPo videoPo = videoMapper.selectById(videoId);
            if (videoPo == null || !Objects.equals(videoPo.getUserId(), userId)) {
                throw new NotFoundException("视频不存在或无权查看评论");
            }
        }

        Long total = commentMapper.countCreatorComments(userId, keyword, videoId);
        if (total == null || total <= 0) {
            return PageVo.<CreatorCommentManageVo>builder()
                    .total(0L)
                    .pageNum(safePageNum)
                    .pageSize(safePageSize)
                    .isEnd(true)
                    .records(Collections.emptyList())
                    .build();
        }

        List<CommentPo> commentList = commentMapper.queryCreatorComments(userId, keyword, videoId, sortType, offset, safePageSize);
        if (CollectionUtils.isEmpty(commentList)) {
            return PageVo.<CreatorCommentManageVo>builder()
                    .total(total)
                    .pageNum(safePageNum)
                    .pageSize(safePageSize)
                    .isEnd(true)
                    .records(Collections.emptyList())
                    .build();
        }

        List<Long> commentIds = commentList.stream().map(CommentPo::getId).collect(Collectors.toList());
        Map<Long, CommentStatsPo> commentStatsMap = queryCommentStatsMap(commentIds);

        Set<Long> relatedUserIds = new HashSet<>();
        Set<Long> relatedVideoIds = new HashSet<>();
        for (CommentPo commentPo : commentList) {
            relatedVideoIds.add(commentPo.getVideoId());
            if (commentPo.getUserId() != null) {
                relatedUserIds.add(commentPo.getUserId());
            }
            if (commentPo.getReplyUserId() != null && commentPo.getReplyUserId() > 0) {
                relatedUserIds.add(commentPo.getReplyUserId());
            }
        }

        Map<Long, UserPo> userMap = relatedUserIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(relatedUserIds).stream()
                .collect(Collectors.toMap(UserPo::getId, item -> item, (left, right) -> left));
        Map<Long, VideoPo> videoMap = relatedVideoIds.isEmpty()
                ? Collections.emptyMap()
                : videoMapper.selectBatchIds(relatedVideoIds).stream()
                .collect(Collectors.toMap(VideoPo::getId, item -> item, (left, right) -> left));

        List<CreatorCommentManageVo> records = commentList.stream()
                .map(comment -> {
                    CommentStatsPo statsPo = commentStatsMap.get(comment.getId());
                    UserPo commenter = userMap.get(comment.getUserId());
                    UserPo replyUser = userMap.get(comment.getReplyUserId());
                    VideoPo targetVideo = videoMap.get(comment.getVideoId());
                    return CreatorCommentManageVo.builder()
                            .commentId(comment.getId())
                            .videoId(comment.getVideoId())
                            .videoTitle(targetVideo == null ? "视频已失效" : targetVideo.getTitle())
                            .videoCoverUrl(targetVideo == null ? null : targetVideo.getCoverUrl())
                            .userId(comment.getUserId())
                            .username(commenter == null ? "未知用户" : commenter.getUsername())
                            .avatarUrl(commenter == null ? null : commenter.getAvatarUrl())
                            .content(comment.getContent())
                            .rootId(comment.getRootId())
                            .parentId(comment.getParentId())
                            .replyUserId(comment.getReplyUserId())
                            .replyUsername(replyUser == null ? null : replyUser.getUsername())
                            .likeCount(statsPo == null ? 0L : defaultLong(statsPo.getLikeCount()))
                            .replyCount(statsPo == null ? 0L : defaultLong(statsPo.getReplyCount()))
                            .isRootComment(Objects.equals(comment.getRootId(), 0L))
                            .createTime(comment.getCreateTime())
                            .build();
                })
                .collect(Collectors.toList());

        return PageVo.<CreatorCommentManageVo>builder()
                .total(total)
                .pageNum(safePageNum)
                .pageSize(safePageSize)
                .isEnd(offset + commentList.size() >= total)
                .records(records)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCreatorComment(Long commentId) {
        Long currentUserId = UserHolderUtil.getUser().getUserId();
        if (commentId == null) {
            throw new ErrorParamException("评论ID不能为空");
        }

        CommentPo commentPo = commentMapper.selectById(commentId);
        if (commentPo == null || !Objects.equals(commentPo.getIsDeleted(), NOT_DELETED)) {
            throw new NotFoundException("评论不存在");
        }

        VideoPo videoPo = videoMapper.selectById(commentPo.getVideoId());
        if (videoPo == null) {
            throw new NotFoundException("评论对应的视频不存在");
        }
        boolean canDelete = Objects.equals(videoPo.getUserId(), currentUserId) || Objects.equals(commentPo.getUserId(), currentUserId);
        if (!canDelete) {
            throw new NoPermissionException("无权限删除该评论");
        }

        doDeleteComment(commentPo);
    }

    private void doDeleteComment(CommentPo commentPo) {
        if (Objects.equals(commentPo.getRootId(), 0L)) {
            int removed = commentMapper.logicDeleteRootTree(commentPo.getId());
            if (removed > 0) {
                videoStatsMapper.updateCommentCount(commentPo.getVideoId(), -1);
            }
            return;
        }

        int removed = commentMapper.logicDeleteById(commentPo.getId());
        if (removed > 0) {
            commentStatsMapper.updateReplyCount(commentPo.getRootId(), -1);
            if (commentPo.getParentId() != null
                    && commentPo.getParentId() > 0
                    && !Objects.equals(commentPo.getParentId(), commentPo.getRootId())) {
                commentStatsMapper.updateReplyCount(commentPo.getParentId(), -1);
            }
        }
    }

    private CommentPageVo listHotComments(Long videoId, Long currentUserId, CommentListRequest request, Integer pageSize) {
        List<CommentStatsPo> statsList = interactRepository.queryRootCommentStatsByHot(
                videoId,
                request.getLastHotScore(),
                request.getLastCreateTime(),
                request.getLastCommentId(),
                pageSize
        );
        Long total = interactRepository.queryTotalRootCommentNum(videoId);
        if (CollectionUtils.isEmpty(statsList)) {
            return CommentPageVo.builder()
                    .total(total == null ? 0L : total)
                    .pageSize(pageSize)
                    .isEnd(true)
                    .records(Collections.emptyList())
                    .build();
        }

        boolean isEnd = statsList.size() <= pageSize;
        List<CommentStatsPo> pageStats = isEnd ? statsList : new ArrayList<>(statsList.subList(0, pageSize));
        List<Long> commentIds = pageStats.stream().map(CommentStatsPo::getCommentId).collect(Collectors.toList());
        Map<Long, CommentPo> commentMap = commentMapper.selectBatchIds(commentIds).stream()
                .collect(Collectors.toMap(CommentPo::getId, item -> item, (left, right) -> left));
        List<CommentPo> orderedComments = commentIds.stream()
                .map(commentMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        PageVo<CommentVo> commentPage = buildCommentPage(orderedComments, total, null, pageSize, currentUserId);
        CommentPageVo pageVo = CommentPageVo.builder()
                .total(commentPage.getTotal())
                .pageNum(commentPage.getPageNum())
                .pageSize(commentPage.getPageSize())
                .records(commentPage.getRecords())
                .build();
        pageVo.setIsEnd(isEnd);
        if (!isEnd && CollectionUtils.isNotEmpty(pageStats)) {
            CommentStatsPo lastStats = pageStats.get(pageStats.size() - 1);
            pageVo.setLastHotScore(lastStats.getHotScore());
            pageVo.setLastCreateTime(lastStats.getCreateTime());
            pageVo.setLastCommentId(lastStats.getCommentId());
        }
        return pageVo;
    }

    private CommentPageVo buildLatestCommentPage(List<CommentPo> queriedComments, Long total, Integer pageSize, Long currentUserId) {
        if (CollectionUtils.isEmpty(queriedComments)) {
            return CommentPageVo.builder()
                    .total(total == null ? 0L : total)
                    .pageSize(pageSize)
                    .isEnd(true)
                    .records(Collections.emptyList())
                    .build();
        }
        boolean isEnd = queriedComments.size() <= pageSize;
        List<CommentPo> pageRecords = isEnd ? queriedComments : new ArrayList<>(queriedComments.subList(0, pageSize));
        PageVo<CommentVo> commentPage = buildCommentPage(pageRecords, total, null, pageSize, currentUserId);
        CommentPageVo pageVo = CommentPageVo.builder()
                .total(commentPage.getTotal())
                .pageNum(commentPage.getPageNum())
                .pageSize(commentPage.getPageSize())
                .records(commentPage.getRecords())
                .build();
        pageVo.setIsEnd(isEnd);
        if (!isEnd && CollectionUtils.isNotEmpty(pageRecords)) {
            CommentPo lastComment = pageRecords.get(pageRecords.size() - 1);
            pageVo.setLastCreateTime(lastComment.getCreateTime());
            pageVo.setLastCommentId(lastComment.getId());
        }
        return pageVo;
    }

    private PageVo<CommentVo> buildCommentPage(List<CommentPo> commentList, Long total, Integer pageNum, Integer pageSize, Long currentUserId) {
        commentList = filterBlockedComments(commentList, currentUserId);
        if (CollectionUtils.isEmpty(commentList)) {
            return PageVo.<CommentVo>builder()
                    .total(total == null ? 0L : total)
                    .pageNum(pageNum)
                    .pageSize(pageSize)
                    .records(Collections.emptyList())
                    .build();
        }

        List<Long> commentIds = commentList.stream().map(CommentPo::getId).collect(Collectors.toList());
        Map<Long, CommentStatsPo> commentStatsMap = queryCommentStatsMap(commentIds);
        Set<Long> relatedUserIds = new HashSet<>();
        for (CommentPo comment : commentList) {
            if (comment.getUserId() != null) {
                relatedUserIds.add(comment.getUserId());
            }
            if (comment.getReplyUserId() != null && comment.getReplyUserId() > 0) {
                relatedUserIds.add(comment.getReplyUserId());
            }
        }

        Map<Long, UserPo> userMap = relatedUserIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(relatedUserIds).stream()
                .collect(Collectors.toMap(UserPo::getId, item -> item, (left, right) -> left));

        Set<Long> likedCommentIdSet = interactRelationService.batchQueryLikeComment(currentUserId, commentIds);
        List<CommentVo> records = commentList.stream().map(comment -> {
            CommentStatsPo statsPo = commentStatsMap.get(comment.getId());
            UserPo userPo = userMap.get(comment.getUserId());
            UserPo replyUserPo = userMap.get(comment.getReplyUserId());
            return CommentVo.builder()
                    .commentId(comment.getId())
                    .userId(comment.getUserId())
                    .username(userPo == null ? "未知用户" : userPo.getUsername())
                    .avatarUrl(userPo == null ? null : userPo.getAvatarUrl())
                    .content(comment.getContent())
                    .rootId(comment.getRootId())
                    .parentId(comment.getParentId())
                    .replyUserId(comment.getReplyUserId())
                    .replyUsername(replyUserPo == null ? null : replyUserPo.getUsername())
                    .replyUserAvatarUrl(replyUserPo == null ? null : replyUserPo.getAvatarUrl())
                    .likeCount(statsPo == null || statsPo.getLikeCount() == null ? 0L : statsPo.getLikeCount())
                    .replyCount(statsPo == null || statsPo.getReplyCount() == null ? 0L : statsPo.getReplyCount())
                    .hotScore(statsPo == null || statsPo.getHotScore() == null ? 10L : statsPo.getHotScore())
                    .isLike(likedCommentIdSet.contains(comment.getId()))
                    .isTop(Objects.equals(comment.getIsTop(), 1))
                    .createTime(comment.getCreateTime())
                    .build();
        }).collect(Collectors.toList());

        attachReplyPreviewList(commentList, records, currentUserId);

        return PageVo.<CommentVo>builder()
                .total(total == null ? 0L : total)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .records(records)
                .build();
    }

    private void attachReplyPreviewList(List<CommentPo> sourceComments, List<CommentVo> builtRecords, Long currentUserId) {
        if (CollectionUtils.isEmpty(sourceComments) || CollectionUtils.isEmpty(builtRecords)) {
            return;
        }
        boolean allRootComments = sourceComments.stream().allMatch(item -> Objects.equals(item.getRootId(), 0L));
        if (!allRootComments) {
            return;
        }

        List<Long> rootCommentIds = sourceComments.stream().map(CommentPo::getId).collect(Collectors.toList());
        List<CommentPo> directReplies = commentMapper.selectList(new LambdaQueryWrapper<CommentPo>()
                .in(CommentPo::getRootId, rootCommentIds)
                .in(CommentPo::getParentId, rootCommentIds)
                .eq(CommentPo::getIsDeleted, NOT_DELETED));
        directReplies = filterBlockedComments(directReplies, currentUserId);
        if (CollectionUtils.isEmpty(directReplies)) {
            return;
        }

        Map<Long, CommentStatsPo> replyStatsMap = queryCommentStatsMap(directReplies.stream().map(CommentPo::getId).collect(Collectors.toList()));
        Map<Long, List<CommentPo>> replyGroupMap = directReplies.stream().collect(Collectors.groupingBy(CommentPo::getRootId));
        Map<Long, List<CommentVo>> previewMap = new HashMap<>();
        replyGroupMap.forEach((rootId, replies) -> {
            List<CommentPo> topReplies = replies.stream()
                    .sorted((left, right) -> {
                        long rightHotScore = getCommentHotScore(replyStatsMap, right.getId());
                        long leftHotScore = getCommentHotScore(replyStatsMap, left.getId());
                        int hotCompare = Long.compare(rightHotScore, leftHotScore);
                        if (hotCompare != 0) {
                            return hotCompare;
                        }
                        return right.getId().compareTo(left.getId());
                    })
                    .limit(2)
                    .sorted(Comparator.comparing(CommentPo::getCreateTime).thenComparing(CommentPo::getId))
                    .collect(Collectors.toList());
            List<CommentVo> previewList = buildCommentPage(topReplies, (long) topReplies.size(), null, topReplies.size(), currentUserId).getRecords();
            previewMap.put(rootId, previewList);
        });
        builtRecords.forEach(item -> item.setReplyPreviewList(previewMap.getOrDefault(item.getCommentId(), Collections.emptyList())));
    }

    private List<CommentPo> filterBlockedComments(List<CommentPo> commentList, Long currentUserId) {
        if (currentUserId == null || CollectionUtils.isEmpty(commentList)) {
            return commentList;
        }
        List<Long> candidateUserIds = commentList.stream()
                .map(CommentPo::getUserId)
                .filter(Objects::nonNull)
                .filter(userId -> !Objects.equals(userId, currentUserId))
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(candidateUserIds)) {
            return commentList;
        }
        List<Long> blockedUserIdList = userBlockMapper.listMutualBlockedUserIds(currentUserId, candidateUserIds);
        if (CollectionUtils.isEmpty(blockedUserIdList)) {
            return commentList;
        }
        Set<Long> blockedUserIds = new HashSet<>(blockedUserIdList);
        return commentList.stream()
                .filter(comment -> !blockedUserIds.contains(comment.getUserId()))
                .collect(Collectors.toList());
    }

    private Map<Long, CommentStatsPo> queryCommentStatsMap(List<Long> commentIds) {
        if (CollectionUtils.isEmpty(commentIds)) {
            return Collections.emptyMap();
        }
        List<CommentStatsPo> statsList = commentStatsMapper.selectList(new LambdaQueryWrapper<CommentStatsPo>()
                .in(CommentStatsPo::getCommentId, commentIds));
        return statsList.stream().collect(Collectors.toMap(CommentStatsPo::getCommentId, item -> item, (left, right) -> left));
    }

    private long getCommentHotScore(Map<Long, CommentStatsPo> commentStatsMap, Long commentId) {
        CommentStatsPo statsPo = commentStatsMap.get(commentId);
        return statsPo == null || statsPo.getHotScore() == null ? 10L : statsPo.getHotScore();
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private Long getCurrentUserIdOrNull() {
        return UserHolderUtil.getUser() == null ? null : UserHolderUtil.getUser().getUserId();
    }
}
