package com.syt.graduationproject.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.syt.graduationproject.mapper.*;
import com.syt.graduationproject.enums.PrivateMessageStatusEnum;
import com.syt.graduationproject.model.bo.FollowBo;
import com.syt.graduationproject.model.bo.UserVideoInteractionBo;
import com.syt.graduationproject.model.po.*;
import com.syt.graduationproject.model.request.CommentRequest;
import com.syt.graduationproject.model.request.FollowRequest;
import com.syt.graduationproject.model.request.LikeRequest;
import com.syt.graduationproject.repository.InteractRepository;
import com.syt.graduationproject.service.InteractService;
import com.syt.graduationproject.util.UserHolderUtil;
import com.syt.graduationproject.websocket.model.PrivateChatMessage;
import com.syt.graduationproject.websocket.model.PrivateChatSendRequest;
import com.syt.graduationproject.websocket.model.WsEnvelope;
import com.syt.graduationproject.util.JsonUtil;
import com.syt.graduationproject.model.vo.ChatSessionVo;
import com.syt.graduationproject.model.vo.PrivateMessageVo;
import com.syt.graduationproject.websocket.ChatRedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.CloseStatus;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

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

    private final CollectionItemMapper collectionItemMapper;

    private final PrivateMessageMapper privateMessageMapper;

    private final StringRedisTemplate stringRedisTemplate;

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

    /**
     * 点赞/取消点赞视频
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeVideo(LikeRequest request) {
        Long myId = UserHolderUtil.getUser().getUserId();
        Long videoId = request.getTargetId();
        Integer operation = request.getOperation();
        if (operation == 1) {
            // --- 执行点赞 ---
            LikeVideoPo likeVideoPo = LikeVideoPo.builder().userId(myId).videoId(videoId).build();
            try {
                likeVideoMapper.insert(likeVideoPo);
                // 插入成功，增加计数
                videoStatsMapper.updateLikeCount(videoId, 1);
            } catch (DuplicateKeyException e) {
                // 幂等处理：如果已经点过赞，直接忽略，不重复加分
                log.warn("用户重复点赞，用户ID：{}，视频ID：{}", myId, videoId);
            }
        } else {
            // --- 执行取消 ---
            QueryWrapper<LikeVideoPo> wrapper = new QueryWrapper<>();
            wrapper.lambda()
                    .eq(LikeVideoPo::getUserId, myId)
                    .eq(LikeVideoPo::getVideoId, videoId);
            int rows = likeVideoMapper.delete(wrapper);
            if (rows > 0) {
                // 只有真正删除了记录，才减少计数
                videoStatsMapper.updateLikeCount(videoId, -1);
            }
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
                        .eq(CollectionItemPo::getVideoId, videoId)) > 0)
                .build();
    }

    @Override
    public void registerChatSession(Long userId, WebSocketSession session) {
        chatSessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
        sessionLastSeen.put(session.getId(), System.currentTimeMillis());
        // 1) 写入 Redis：userId -> sessionId（用于在线判断/多实例扩展的基础）
        stringRedisTemplate.opsForValue().set(ChatRedisKeys.onlineKey(userId), session.getId(), ONLINE_TTL);
        // 2) 上线即拉取离线消息（Redis List）并顺序推送，推完批量标已读并清空
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
            stringRedisTemplate.delete(ChatRedisKeys.onlineKey(userId));
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
        LocalDateTime now = LocalDateTime.now();

        // 1) 落库（生成 serverMsgId）——MySQL 兜底：无论在线/离线都先写入历史
        PrivateMessagePo po = PrivateMessagePo.builder()
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .clientMsgId(request.getClientMsgId())
                .content(content)
                .status(PrivateMessageStatusEnum.SAVED.getCode())
                .createTime(now)
                .updateTime(now)
                .build();
        try {
            privateMessageMapper.insert(po);
        } catch (DuplicateKeyException e) {
            // 幂等：相同 from_user_id + client_msg_id 重复发送时，直接查回已有记录并继续推送
            PrivateMessagePo existed = privateMessageMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PrivateMessagePo>()
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
                .sendTime(now)
                .build();

        WsEnvelope<PrivateChatMessage> chatEnvelope = WsEnvelope.<PrivateChatMessage>builder()
                .type("chat")
                .data(chatMsg)
                .build();

        // 2) 在线判断：优先用 Redis 判断是否在线；若在线则实时转发，否则写离线队列
        boolean isOnline = stringRedisTemplate.hasKey(ChatRedisKeys.onlineKey(toUserId));
        boolean delivered = false;
        if (isOnline) {
            delivered = broadcastToUser(toUserId, chatEnvelope);
        }
        if (!delivered) {
            // 离线：写入 Redis List（按顺序）——缓存加速
            String offlineJson = JsonUtil.toJson(chatEnvelope);
            stringRedisTemplate.opsForList().rightPush(ChatRedisKeys.offlineListKey(toUserId), offlineJson);
        }

        // 3) 更新 MySQL 状态：在线投递成功 -> DELIVERED；离线则保持“未读”（即非 READ）
        if (delivered) {
            PrivateMessagePo update = PrivateMessagePo.builder()
                    .status(PrivateMessageStatusEnum.DELIVERED.getCode())
                    .deliveredTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            privateMessageMapper.update(update,
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PrivateMessagePo>()
                            .eq(PrivateMessagePo::getId, serverMsgId));
        }

        // 4) 给发送方推送 ack（包含是否实时投递成功）
        broadcastToUser(fromUserId, WsEnvelope.<Object>builder()
                .type("ack")
                .data(new AckPayload(request.getClientMsgId(), serverMsgId, toUserId, delivered))
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


    @lombok.Data
    @lombok.AllArgsConstructor
    private static class AckPayload {
        private String clientMsgId;
        private Long serverMsgId;
        private Long toUserId;
        private Boolean delivered;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ReadPayload {
        private Long readByUserId;
        private Long upToMsgId;
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
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PrivateMessagePo>()
                        .eq(PrivateMessagePo::getId, serverMsgId)
                        .eq(PrivateMessagePo::getToUserId, userId));
    }

    @Override
    public void heartbeat(Long userId, WebSocketSession session) {
        if (session != null) {
            sessionLastSeen.put(session.getId(), System.currentTimeMillis());
        }
        // 心跳刷新在线 TTL（Redis）
        if (userId != null) {
            stringRedisTemplate.expire(ChatRedisKeys.onlineKey(userId), ONLINE_TTL);
        }
    }

    /**
     * 用户上线后拉取离线消息：按顺序推送 -> 批量更新 DB 为 READ -> 清空 Redis List
     */
    public void deliverOfflineMessages(Long userId) {
        if (userId == null) {
            return;
        }
        String listKey = ChatRedisKeys.offlineListKey(userId);
        Long size = stringRedisTemplate.opsForList().size(listKey);
        if (size == null || size <= 0) {
            return;
        }

        List<String> items = stringRedisTemplate.opsForList().range(listKey, 0, -1);
        if (items == null || items.isEmpty()) {
            return;
        }

        List<Long> msgIds = new ArrayList<>();
        for (String json : items) {
            if (json == null || json.trim().isEmpty()) {
                continue;
            }
            // 推送给当前用户（按顺序）
            broadcastToUser(userId, JsonUtil.fromJson(json, WsEnvelope.class));

            // 收集 serverMsgId，用于批量标记已读
            try {
                // 先把 envelope 转成 Map 再提取 data.serverMsgId（避免泛型问题）
                @SuppressWarnings("unchecked")
                Map<String, Object> raw = (Map<String, Object>) JsonUtil.fromJson(json, Map.class);
                Object data = raw.get("data");
                if (data instanceof Map) {
                    Map<?, ?> dataMap = (Map<?, ?>) data;
                    Object serverMsgId = dataMap.get("serverMsgId");
                    if (serverMsgId != null) {
                        msgIds.add(Long.valueOf(String.valueOf(serverMsgId)));
                    }
                }
            } catch (Exception ignore) {
            }
        }

        if (!msgIds.isEmpty()) {
            privateMessageMapper.markReadByIds(
                    userId,
                    msgIds.stream().distinct().collect(Collectors.toList()),
                    PrivateMessageStatusEnum.READ.getCode(),
                    LocalDateTime.now()
            );
        }

        // 最后清空离线队列
        stringRedisTemplate.delete(listKey);
    }

    @Override
    public List<PrivateMessageVo> queryChatHistory(Long myId, Long withUserId, Long beforeId, Integer pageSize) {
        int size = pageSize == null ? 20 : Math.min(Math.max(pageSize, 1), 100);
        return privateMessageMapper.queryHistory(myId, withUserId, beforeId, size);
    }

    @Override
    public List<ChatSessionVo> queryChatSessions(Long myId) {
        return privateMessageMapper.querySessions(myId, PrivateMessageStatusEnum.READ.getCode());
    }

    @Override
    public Long queryTotalUnread(Long myId) {
        return privateMessageMapper.queryTotalUnread(myId, PrivateMessageStatusEnum.READ.getCode());
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
}
