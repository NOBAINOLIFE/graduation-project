package com.syt.graduationproject.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syt.graduationproject.enums.*;
import com.syt.graduationproject.exception.CustomException;
import com.syt.graduationproject.mapper.*;
import com.syt.graduationproject.model.bo.FollowBo;
import com.syt.graduationproject.model.bo.UserVideoInteractionBo;
import com.syt.graduationproject.model.po.*;
import com.syt.graduationproject.model.request.*;
import com.syt.graduationproject.model.vo.*;
import com.syt.graduationproject.model.websocket.*;
import com.syt.graduationproject.repository.InteractRepository;
import com.syt.graduationproject.repository.VideoRepository;
import com.syt.graduationproject.service.EsSyncService;
import com.syt.graduationproject.service.InteractService;
import com.syt.graduationproject.util.JsonUtil;
import com.syt.graduationproject.util.RedisKeyUtil;
import com.syt.graduationproject.util.UserHolderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.dao.DuplicateKeyException;
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

    private final CoinRecordMapper coinRecordMapper;

    private final CollectionDirectoryMapper collectionDirectoryMapper;

    private final CollectionItemMapper collectionItemMapper;

    private final UserBlockMapper userBlockMapper;

    private final UserWalletMapper userWalletMapper;

    private final UserRewardLogMapper userRewardLogMapper;

    private final PrivateMessageMapper privateMessageMapper;

    private final ReportMapper reportMapper;

    private final VideoMapper videoMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final VideoRepository videoRepository;

    private final EsSyncService esSyncService;

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

    private final UserMapper userMapper;

    private static final int OPERATION_ON = 1;

    private static final int OPERATION_OFF = 0;

    private static final int DAILY_LOGIN_REWARD = 1;

    private static final String DEFAULT_COLLECTION_NAME = "默认收藏夹";

    /**
     * 查询两者关注关系
     */
    @Override
    public FollowBo queryFollow(Long followerId, Long followeeId) {
        FollowBo followBo = new FollowBo();

        // 1. 查询当前用户(followerId)是否关注了目标用户(followeeId)
        FollowRecordPo followRecordPo1 = interactRepository.queryFollow(followerId, followeeId);
        followBo.setIsFollow(followRecordPo1 != null && followRecordPo1.getIsDeleted().equals(NOT_DELETED));

        // 2. 查询目标用户(followeeId)是否关注了当前用户(followerId)
        FollowRecordPo followRecordPo2 = interactRepository.queryFollow(followeeId, followerId);
        followBo.setIsFans(followRecordPo2 != null && followRecordPo2.getIsDeleted().equals(NOT_DELETED));

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
        boolean fansChanged = false;
        if (followeeId == null || operation == null) {
            throw new CustomException("关注参数不完整");
        }
        if (Objects.equals(myId, followeeId)) {
            throw new CustomException("不能关注自己");
        }
        if (operation != OPERATION_ON && operation != OPERATION_OFF) {
            throw new CustomException("关注操作类型非法");
        }
        if (hasMutualBlock(myId, followeeId)) {
            throw new CustomException("由于隐私设置，无法关注该用户");
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
            throw new CustomException("拉黑参数不完整");
        }
        Long targetUserId = request.getTargetUserId();
        if (Objects.equals(myId, targetUserId)) {
            throw new CustomException("不能拉黑自己");
        }
        UserPo targetUser = userMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new CustomException("用户不存在");
        }
        if (request.getOperation() != OPERATION_ON && request.getOperation() != OPERATION_OFF) {
            throw new CustomException("拉黑操作类型非法");
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
            throw new CustomException("视频不存在");
        }
        if (!videoPo.getStatus().equals(VideoStatusEnum.PUBLISHED.getCode())) {
            throw new CustomException("视频状态异常");
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
            throw new CustomException("操作类型非法");
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

    /**
     * 查询用户视频互动信息
     */
    @Override
    public UserVideoInteractionBo queryUserVideoInteraction(Long userId, Long videoId) {
        return UserVideoInteractionBo.builder()
                .userId(userId)
                .videoId(videoId)
                .isLike(likeVideoMapper.selectCount(new QueryWrapper<LikeVideoPo>().lambda()
                        .eq(LikeVideoPo::getUserId, userId)
                        .eq(LikeVideoPo::getVideoId, videoId)) > 0)
                .isCoin(coinRecordMapper.selectCount(new QueryWrapper<CoinRecordPo>().lambda()
                        .eq(CoinRecordPo::getUserId, userId)
                        .eq(CoinRecordPo::getVideoId, videoId)) > 0)
                .isCollect(collectionItemMapper.selectCount(new QueryWrapper<CollectionItemPo>().lambda()
                        .eq(CollectionItemPo::getUserId, userId)
                        .eq(CollectionItemPo::getVideoId, videoId)
                        .eq(CollectionItemPo::getIsDeleted, NOT_DELETED)) > 0)
                .build();
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
        if (hasMutualBlock(fromUserId, toUserId)) {
            throw new CustomException("由于隐私设置，无法发送私信");
        }

        // 1) 落库（生成 serverMsgId）——MySQL 兜底：无论在线/离线都先写入历史
        PrivateMessagePo po = PrivateMessagePo.builder()
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .clientMsgId(request.getClientMsgId())
                .content(content)
                .status(PrivateMessageStatusEnum.SAVED.getCode())
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
                po = existed;
            } else {
                throw e;
            }
        }

        Long serverMsgId = po.getId();
        PrivateChatMessage chatMsg = PrivateChatMessage.builder()
                .serverMsgId(serverMsgId)
                .clientMsgId(request.getClientMsgId())
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .content(content)
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
                .data(new SendAckPayload(request.getClientMsgId(), serverMsgId, toUserId, delivered))
                .build());
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
        // 只有接收方才能 ack（userId == toUserId）
        PrivateMessagePo update = PrivateMessagePo.builder()
                .status(PrivateMessageStatusEnum.ACKED.getCode())
                .ackedTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        privateMessageMapper.update(update,
                new LambdaQueryWrapper<PrivateMessagePo>()
                        .eq(PrivateMessagePo::getId, serverMsgId)
                        .eq(PrivateMessagePo::getToUserId, userId)
                        // 仅允许 SAVED/DELIVERED -> ACKED，避免 READ 被回退为 ACKED
                        .in(PrivateMessagePo::getStatus,
                                PrivateMessageStatusEnum.SAVED.getCode(),
                                PrivateMessageStatusEnum.DELIVERED.getCode()));
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
        if (hasMutualBlock(myId, withUserId)) {
            throw new CustomException("由于隐私设置，无法查看私信");
        }
        int size = pageSize == null ? 20 : Math.min(Math.max(pageSize, 1), 100);
        return privateMessageMapper.queryHistory(myId, withUserId, beforeId, size);
    }

    @Override
    public List<ChatSessionVo> queryChatSessions(Long myId) {
        List<ChatSessionVo> sessions = privateMessageMapper.querySessions(myId, PrivateMessageStatusEnum.READ.getCode());
        if (sessions == null || sessions.isEmpty()) {
            return sessions;
        }
        return sessions.stream()
                .filter(item -> !hasMutualBlock(myId, item.getWithUserId()))
                .collect(Collectors.toList());
    }

    @Override
    public Long queryTotalUnread(Long myId) {
        return privateMessageMapper.queryTotalUnread(myId, PrivateMessageStatusEnum.READ.getCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markChatRead(Long myId, Long withUserId, Long upToMsgId) {
        if (hasMutualBlock(myId, withUserId)) {
            throw new CustomException("由于隐私设置，无法操作私信");
        }
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
    public List<UserSimpleInfoVo> queryFansList(Long userId) {
        // 查询粉丝列表
        List<FollowRecordPo> fans = followRecordMapper.selectList(new QueryWrapper<FollowRecordPo>().lambda()
                .eq(FollowRecordPo::getFolloweeId, userId)
                .eq(FollowRecordPo::getIsDeleted, NOT_DELETED));
        if (CollectionUtils.isEmpty(fans)) {
            return Collections.emptyList();
        }
        // 判断是否关注粉丝
        List<Long> fanIds = fans.stream().map(FollowRecordPo::getFollowerId).collect(Collectors.toList());
        List<FollowRecordPo> myFollows = followRecordMapper.selectList(new QueryWrapper<FollowRecordPo>().lambda()
                .eq(FollowRecordPo::getFollowerId, userId)
                .in(FollowRecordPo::getFolloweeId, fanIds)
                .eq(FollowRecordPo::getIsDeleted, NOT_DELETED));
        Set<Long> followedFanIds = myFollows.stream().map(FollowRecordPo::getFolloweeId).collect(Collectors.toSet());
        // 构造返回列表
        List<UserSimpleInfoVo> result = new ArrayList<>();
        List<UserPo> fansInfoList = userMapper.selectList(new QueryWrapper<UserPo>().lambda()
                .in(UserPo::getId, fanIds));
        for (UserPo fansInfo : fansInfoList) {
            if (hasMutualBlock(userId, fansInfo.getId())) {
                continue;
            }
            UserSimpleInfoVo vo = new UserSimpleInfoVo();
            vo.setUserId(fansInfo.getId());
            vo.setUsername(fansInfo.getUsername());
            vo.setBio(fansInfo.getBio());
            vo.setAvatarUrl(fansInfo.getAvatarUrl());
            vo.setIsFollow(followedFanIds.contains(fansInfo.getId()));
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<UserSimpleInfoVo> queryFollowList(Long userId) {
        // 查询关注列表
        List<FollowRecordPo> follows = followRecordMapper.selectList(new QueryWrapper<FollowRecordPo>().lambda()
                .eq(FollowRecordPo::getFollowerId, userId)
                .eq(FollowRecordPo::getIsDeleted, NOT_DELETED));
        if (CollectionUtils.isEmpty(follows)) {
            return Collections.emptyList();
        }
        // 构造返回列表
        List<UserSimpleInfoVo> result = new ArrayList<>();
        List<Long> followsIdList = follows.stream()
                .map(FollowRecordPo::getFolloweeId)
                .collect(Collectors.toList());
        List<UserPo> followsInfoList = userMapper.selectList(new QueryWrapper<UserPo>().lambda()
                .in(UserPo::getId, followsIdList));
        for (UserPo followsInfo : followsInfoList) {
            if (hasMutualBlock(userId, followsInfo.getId())) {
                continue;
            }
            UserSimpleInfoVo vo = new UserSimpleInfoVo();
            vo.setUserId(followsInfo.getId());
            vo.setUsername(followsInfo.getUsername());
            vo.setBio(followsInfo.getBio());
            vo.setAvatarUrl(followsInfo.getAvatarUrl());
            vo.setIsFollow(true);
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void collectVideo(CollectVideoRequest request) {
        Long userId = UserHolderUtil.getUser().getUserId();
        if (request == null || request.getVideoId() == null) {
            throw new CustomException("收藏参数不完整");
        }
        Long videoId = request.getVideoId();
        List<Long> collectDirectoryIdList = request.getCollectDirectoryIdList();
        List<Long> removeDirectoryIdList = request.getRemoveDirectoryIdList();

        VideoPo videoPo = videoMapper.selectById(videoId);
        if (videoPo == null || videoPo.getStatus() != VideoStatusEnum.PUBLISHED.getCode()) {
            throw new CustomException("视频不存在或不可收藏");
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
        boolean beforeCollect = interactRepository.isCollectVideo(userId, videoId);

        // --- 执行收藏 ---
        for (Long directoryId : CollectionUtils.emptyIfNull(collectDirectoryIdList)) {
            // 如果有收藏记录
            List<CollectionItemPo> collectionItemPos = interactRepository.queryUserCollectRecord(userId, videoId, directoryId);
            if (CollectionUtils.isNotEmpty(collectionItemPos)) {
                CollectionItemPo itemPo = collectionItemPos.get(0);
                if (itemPo.getIsDeleted().equals(NOT_DELETED)) {
                    throw new CustomException("该收藏夹已收藏该视频");
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
        boolean afterCollect = interactRepository.isCollectVideo(userId, videoId);
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
            throw new CustomException("举报参数不完整");
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new CustomException("举报原因不能为空");
        }

        Integer targetType = request.getTargetType();
        Long targetId = request.getTargetId();
        if (ReportTargetTypeEnum.USER.getCode().equals(targetType)) {
            if (Objects.equals(reporterId, targetId)) {
                throw new CustomException("不能举报自己");
            }
            UserPo targetUser = userMapper.selectById(targetId);
            if (targetUser == null) {
                throw new CustomException("被举报用户不存在");
            }
        } else if (ReportTargetTypeEnum.VIDEO.getCode().equals(targetType)) {
            VideoPo targetVideo = videoMapper.selectById(targetId);
            if (targetVideo == null) {
                throw new CustomException("被举报视频不存在");
            }
        } else if (ReportTargetTypeEnum.COMMENT.getCode().equals(targetType)) {
            CommentPo targetComment = commentMapper.selectById(targetId);
            if (targetComment == null || !NOT_DELETED.equals(targetComment.getIsDeleted())) {
                throw new CustomException("被举报评论不存在");
            }
        } else {
            throw new CustomException("举报类型非法");
        }

        ReportPo reportPo = ReportPo.builder()
                .reporterId(reporterId)
                .targetType(targetType)
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
            throw new CustomException("收藏夹名称不能为空");
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
            throw new CustomException("收藏夹参数不完整");
        }
        CollectionDirectoryPo po = queryOwnedDirectory(myId, request.getDirectoryId());
        if (po == null) {
            throw new CustomException("收藏夹不存在或无权限");
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
            throw new CustomException("由于隐私设置，无法查看收藏夹");
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
            throw new CustomException("视频ID不能为空");
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
            throw new CustomException("批量操作参数不完整");
        }
        CollectionDirectoryPo sourceDirectory = queryOwnedDirectory(userId, request.getSourceDirectoryId());
        if (sourceDirectory == null) {
            throw new CustomException("来源收藏夹不存在或无权限");
        }
        CollectionDirectoryPo targetDirectory = queryOwnedDirectory(userId, request.getTargetDirectoryId());
        if (targetDirectory == null) {
            throw new CustomException("目标收藏夹不存在或无权限");
        }

        Integer operation = request.getOperation();
        List<Long> videoIdList = request.getVideoIds();

        // 批量取消收藏
        if (operation.equals(BatchCollectOperationEnum.CANCEL_COLLECT.getCode())) {
            int canceled1 = interactRepository.batchCollectVideo(userId, sourceDirectory.getId(), videoIdList);
            int canceled2 = videoRepository.batchAddVideoCollectCount(videoIdList, -1L);
            if (canceled1 != videoIdList.size() || canceled2 != videoIdList.size()) {
                throw new CustomException("批量操作失败");
            }
            return canceled1;
        }

        // 批量复制
        if (operation.equals(BatchCollectOperationEnum.COPY.getCode())) {
            int added = interactRepository.batchCollectVideo(userId, targetDirectory.getId(), videoIdList);
            if (added != videoIdList.size()) {
                throw new CustomException("批量操作失败");
            }
            return added;
        }

        // 批量移动
        if (operation.equals(BatchCollectOperationEnum.MOVE.getCode())) {
            int canceled = interactRepository.batchCancelCollectVideo(userId, sourceDirectory.getId(), videoIdList);
            int added = interactRepository.batchCollectVideo(userId, targetDirectory.getId(), videoIdList);
            if (canceled != added) {
                throw new CustomException("批量操作失败");
            }
            return canceled;
        }

        throw new CustomException("批量操作参数错误");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer clearInvalidCollectionItems(Long directoryId) {
        Long myId = UserHolderUtil.getUser().getUserId();
        if (directoryId == null) {
            throw new CustomException("收藏夹ID不能为空");
        }
        CollectionDirectoryPo directoryPo = queryOwnedDirectory(myId, directoryId);
        if (directoryPo == null) {
            throw new CustomException("收藏夹不存在或无权限");
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
            throw new CustomException("投币参数不完整");
        }
        if (request.getAmount() != 1 && request.getAmount() != 2) {
            throw new CustomException("投币数量只能为1或2");
        }
        VideoPo videoPo = videoMapper.selectById(request.getVideoId());
        if (videoPo == null || videoPo.getStatus() != VideoStatusEnum.PUBLISHED.getCode()) {
            throw new CustomException("视频不存在或不可投币");
        }

        ensureWalletRow(myId);
        CoinRecordPo oldRecord = coinRecordMapper.selectOne(new LambdaQueryWrapper<CoinRecordPo>()
                .eq(CoinRecordPo::getUserId, myId)
                .eq(CoinRecordPo::getVideoId, request.getVideoId()));
        int oldAmount = oldRecord == null ? 0 : oldRecord.getAmount();
        int targetAmount = oldAmount + request.getAmount();
        if (targetAmount > 2) {
            throw new CustomException("单个视频最多投币2个");
        }

        int deducted = userWalletMapper.deductCoinIfEnough(myId, request.getAmount().longValue());
        if (deducted <= 0) {
            throw new CustomException("硬币余额不足");
        }

        if (oldRecord == null) {
            coinRecordMapper.insert(CoinRecordPo.builder()
                    .userId(myId)
                    .videoId(request.getVideoId())
                    .amount(request.getAmount())
                    .build());
        } else {
            oldRecord.setAmount(targetAmount);
            coinRecordMapper.updateById(oldRecord);
        }
        videoStatsMapper.updateCoinCount(request.getVideoId(), request.getAmount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean claimDailyCoin(Long userId) {
        if (userId == null) {
            return false;
        }
        ensureWalletRow(userId);
        LocalDate today = LocalDate.now();
        UserRewardLogPo exists = userRewardLogMapper.selectOne(new LambdaQueryWrapper<UserRewardLogPo>()
                .eq(UserRewardLogPo::getUserId, userId)
                .eq(UserRewardLogPo::getRewardDate, today)
                .last("limit 1"));
        if (exists != null) {
            return false;
        }
        userRewardLogMapper.insert(UserRewardLogPo.builder()
                .userId(userId)
                .rewardDate(today)
                .rewardCoin(DAILY_LOGIN_REWARD)
                .build());
        userWalletMapper.updateCoinBalance(userId, (long) DAILY_LOGIN_REWARD);
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
            throw new CustomException("一键三连参数不完整");
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
    public List<ReportVo> listMyReports() {
        Long myId = UserHolderUtil.getUser().getUserId();
        LambdaQueryWrapper<ReportPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportPo::getReporterId, myId)
                .orderByDesc(ReportPo::getCreateTime);
        List<ReportPo> reportList = reportMapper.selectList(wrapper);
        if (reportList == null || reportList.isEmpty()) {
            return Collections.emptyList();
        }
        return reportList.stream().map(po -> ReportVo.builder()
                .reportId(po.getId())
                .targetType(po.getTargetType())
                .targetId(po.getTargetId())
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

        List<CollectionItemPo> items = collectionItemMapper.selectList(new LambdaQueryWrapper<CollectionItemPo>().eq(CollectionItemPo::getDirectoryId, directoryId).eq(CollectionItemPo::getIsDeleted, NOT_DELETED).orderByDesc(CollectionItemPo::getCreateTime));

        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> videoIds = items.stream().map(CollectionItemPo::getVideoId).filter(Objects::nonNull).distinct().collect(Collectors.toList());

        if (videoIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<VideoPo> videos = videoMapper.selectBatchIds(videoIds);
        Map<Long, VideoPo> videoMap = videos.stream().collect(Collectors.toMap(VideoPo::getId, v -> v));

        Set<Long> userIds = videos.stream().map(VideoPo::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, UserPo> userMap = userIds.isEmpty() ? Collections.emptyMap() : userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(UserPo::getId, u -> u));

        List<VideoStatsPo> statsList = videoStatsMapper.selectList(new LambdaQueryWrapper<VideoStatsPo>().in(VideoStatsPo::getVideoId, videoIds));
        Map<Long, VideoStatsPo> statsMap = statsList.stream().collect(Collectors.toMap(VideoStatsPo::getVideoId, s -> s));

        List<SearchVideoVo> result = items.stream().map(item -> {
            VideoPo video = videoMap.get(item.getVideoId());
            if (video == null) return null;
            UserPo user = userMap.get(video.getUserId());
            VideoStatsPo stats = statsMap.get(video.getId());

            return SearchVideoVo.builder().videoId(video.getId()).title(video.getTitle()).userId(video.getUserId()).username(user != null ? user.getUsername() : "未知用户").coverUrl(video.getCoverUrl()).playCount(stats != null ? stats.getPlayCount() : 0L).createTime(video.getCreateTime()).collectionCount(stats != null ? stats.getCollectCount() : 0L).duration(video.getDuration()).collectTime(item.getCreateTime()).build();
        }).filter(Objects::nonNull).collect(Collectors.toList());

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
            throw new CustomException("收藏夹ID不能为空");
        }
        CollectionDirectoryPo directoryPo = queryOwnedDirectory(myId, directoryId);
        if (directoryPo == null) {
            throw new CustomException("收藏夹不存在或无权限");
        }
        if (Objects.equals(directoryPo.getIsDefault(), 1)) {
            throw new CustomException("默认收藏夹无法删除");
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

    @Override
    public PageVo<CommentVo> listVideoComments(Long videoId, Integer sortType, Integer pageNum, Integer pageSize) {
        if (videoId == null) {
            throw new CustomException("视频ID不能为空");
        }
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null ? 10 : Math.min(Math.max(pageSize, 1), 50);
        VideoPo videoPo = videoMapper.selectById(videoId);
        if (videoPo == null || !Objects.equals(videoPo.getStatus(), VideoStatusEnum.PUBLISHED.getCode())) {
            throw new CustomException("视频不存在或未发布");
        }

        Long myId = getCurrentUserIdOrNull();
        if (Objects.equals(sortType, 1)) {
            return listHotComments(videoId, myId, safePageNum, safePageSize);
        }
        Page<CommentPo> page = new Page<CommentPo>(safePageNum, safePageSize);
        LambdaQueryWrapper<CommentPo> wrapper = new LambdaQueryWrapper<CommentPo>()
                .eq(CommentPo::getVideoId, videoId)
                .eq(CommentPo::getRootId, 0L)
                .eq(CommentPo::getIsDeleted, NOT_DELETED)
                .orderByDesc(CommentPo::getCreateTime)
                .orderByDesc(CommentPo::getId);
        commentMapper.selectPage(page, wrapper);

        return buildCommentPage(page.getRecords(), page.getTotal(), safePageNum, safePageSize, myId);
    }

    private PageVo<CommentVo> listHotComments(Long videoId, Long currentUserId, Integer pageNum, Integer pageSize) {
        List<CommentPo> allComments = commentMapper.selectList(new LambdaQueryWrapper<CommentPo>()
                .eq(CommentPo::getVideoId, videoId)
                .eq(CommentPo::getRootId, 0L)
                .eq(CommentPo::getIsDeleted, NOT_DELETED)
                .orderByDesc(CommentPo::getCreateTime)
                .orderByDesc(CommentPo::getId));

        if (allComments == null || allComments.isEmpty()) {
            return PageVo.<CommentVo>builder()
                    .total(0L)
                    .pageNum(pageNum)
                    .pageSize(pageSize)
                    .records(Collections.<CommentVo>emptyList())
                    .build();
        }

        List<Long> commentIds = allComments.stream().map(CommentPo::getId).collect(Collectors.toList());
        Map<Long, CommentStatsPo> commentStatsMap = queryCommentStatsMap(commentIds);
        allComments.sort((left, right) -> {
            long rightLikeCount = getCommentLikeCount(commentStatsMap, right.getId());
            long leftLikeCount = getCommentLikeCount(commentStatsMap, left.getId());
            int likeCompare = Long.compare(rightLikeCount, leftLikeCount);
            if (likeCompare != 0) {
                return likeCompare;
            }
            int timeCompare = right.getCreateTime().compareTo(left.getCreateTime());
            if (timeCompare != 0) {
                return timeCompare;
            }
            return right.getId().compareTo(left.getId());
        });

        int fromIndex = Math.min((pageNum - 1) * pageSize, allComments.size());
        int toIndex = Math.min(fromIndex + pageSize, allComments.size());
        List<CommentPo> pageRecords = fromIndex >= toIndex
                ? Collections.<CommentPo>emptyList()
                : new ArrayList<CommentPo>(allComments.subList(fromIndex, toIndex));
        return buildCommentPage(pageRecords, (long) allComments.size(), pageNum, pageSize, currentUserId);
    }

    private PageVo<CommentVo> buildCommentPage(List<CommentPo> commentList, Long total, Integer pageNum, Integer pageSize, Long currentUserId) {
        if (commentList == null || commentList.isEmpty()) {
            return PageVo.<CommentVo>builder()
                    .total(total == null ? 0L : total)
                    .pageNum(pageNum)
                    .pageSize(pageSize)
                    .records(Collections.<CommentVo>emptyList())
                    .build();
        }

        List<Long> commentIds = commentList.stream().map(CommentPo::getId).collect(Collectors.toList());
        Map<Long, CommentStatsPo> commentStatsMap = queryCommentStatsMap(commentIds);
        Set<Long> relatedUserIds = new HashSet<Long>();
        for (CommentPo comment : commentList) {
            if (comment.getUserId() != null) {
                relatedUserIds.add(comment.getUserId());
            }

            if (comment.getReplyUserId() != null && comment.getReplyUserId() > 0) {
                relatedUserIds.add(comment.getReplyUserId());
            }
        }

        Map<Long, UserPo> userMap;
        if (!relatedUserIds.isEmpty()) {
            userMap = userMapper.selectBatchIds(relatedUserIds).stream()
                    .collect(Collectors.toMap(UserPo::getId, item -> item, (left, right) -> left));
        } else {
            userMap = Collections.emptyMap();
        }

        Set<Long> likedCommentIdSet = queryLikedCommentIdSet(currentUserId, commentIds);
        List<CommentVo> records = commentList.stream().map(comment -> {
            CommentStatsPo statsPo = commentStatsMap.get(comment.getId());
            UserPo userPo = userMap.get(comment.getUserId());
            UserPo replyUserPo = userMap.get(comment.getReplyUserId());
            return CommentVo.builder()
                    .commentId(comment.getId())
                    .userId(comment.getUserId())
                    .username(userPo.getUsername())
                    .avatarUrl(userPo.getAvatarUrl())
                    .content(comment.getContent())
                    .rootId(comment.getRootId())
                    .parentId(comment.getParentId())
                    .replyUserId(comment.getReplyUserId())
                    .replyUsername(replyUserPo == null ? null : replyUserPo.getUsername())
                    .replyUserAvatarUrl(replyUserPo == null ? null : replyUserPo.getAvatarUrl())
                    .likeCount(statsPo == null || statsPo.getLikeCount() == null ? 0L : statsPo.getLikeCount())
                    .replyCount(statsPo == null || statsPo.getReplyCount() == null ? 0L : statsPo.getReplyCount())
                    .isLike(likedCommentIdSet.contains(comment.getId()))
                    .createTime(comment.getCreateTime())
                    .build();
        }).collect(Collectors.toList());

        return PageVo.<CommentVo>builder()
                .total(total == null ? 0L : total)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .records(records)
                .build();
    }

    private Map<Long, CommentStatsPo> queryCommentStatsMap(List<Long> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<CommentStatsPo> statsList = commentStatsMapper.selectList(new LambdaQueryWrapper<CommentStatsPo>()
                .in(CommentStatsPo::getCommentId, commentIds));
        return statsList.stream()
                .collect(Collectors.toMap(CommentStatsPo::getCommentId, item -> item, (left, right) -> left));
    }

    private Set<Long> queryLikedCommentIdSet(Long currentUserId, List<Long> commentIds) {
        if (currentUserId == null || commentIds == null || commentIds.isEmpty()) {
            return Collections.emptySet();
        }

        List<LikeCommentPo> likeCommentPos = likeCommentMapper.selectList(new LambdaQueryWrapper<LikeCommentPo>()
                .eq(LikeCommentPo::getUserId, currentUserId)
                .in(LikeCommentPo::getCommentId, commentIds));
        return likeCommentPos.stream().map(LikeCommentPo::getCommentId).collect(Collectors.toSet());
    }

    private long getCommentLikeCount(Map<Long, CommentStatsPo> commentStatsMap, Long commentId) {
        CommentStatsPo statsPo = commentStatsMap.get(commentId);
        return statsPo == null || statsPo.getLikeCount() == null ? 0L : statsPo.getLikeCount();
    }

    private Long getCurrentUserIdOrNull() {
        return UserHolderUtil.getUser() == null ? null : UserHolderUtil.getUser().getUserId();
    }
}
