package com.syt.graduationproject.service;

import com.syt.graduationproject.model.bo.FollowBo;
import com.syt.graduationproject.model.bo.UserVideoInteractionBo;
import com.syt.graduationproject.model.request.CommentRequest;
import com.syt.graduationproject.model.request.FollowRequest;
import com.syt.graduationproject.model.request.LikeRequest;
import com.syt.graduationproject.model.websocket.PrivateChatSendRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.WebSocketSession;
import com.syt.graduationproject.model.vo.ChatSessionVo;
import com.syt.graduationproject.model.vo.PrivateMessageVo;

import java.util.List;

public interface InteractService {

    /**
     * 查询关注关系
     */
    FollowBo queryFollow(Long followerId, Long followeeId);

    /**
     * 关注/取关
     */
    @Transactional(rollbackFor = Exception.class)
    void follow(FollowRequest request);

    /**
     * 评论/回复评论
     */
    @Transactional(rollbackFor = Exception.class)
    void comment(CommentRequest request);

    /**
     * 点赞/取消点赞视频
     */
    @Transactional(rollbackFor = Exception.class)
    void likeVideo(LikeRequest request);

    /**
     * 点赞/取消点赞评论
     */
    @Transactional(rollbackFor = Exception.class)
    void likeComment(LikeRequest request);


    /**
     * 查询用户与视频的交互信息
     */
    UserVideoInteractionBo queryUserVideoInteraction(Long userId, Long videoId);

    /**
     * WebSocket：注册在线会话
     */
    void registerChatSession(Long userId, WebSocketSession session);

    /**
     * WebSocket：移除在线会话
     */
    void removeChatSession(Long userId, WebSocketSession session);

    /**
     * WebSocket：发送私聊（由 WebSocketHandler 调用）
     */
    void sendPrivateChat(Long fromUserId, PrivateChatSendRequest request);

    /**
     * WebSocket：客户端消息确认（收到 chat 后回 ack）
     */
    void ackPrivateMessage(Long userId, Long serverMsgId);

    /**
     * WebSocket：心跳
     */
    void heartbeat(Long userId, WebSocketSession session);

    /**
     * 拉取与某用户的历史消息（倒序返回）
     */
    List<PrivateMessageVo> queryChatHistory(Long myId, Long withUserId, Long beforeId, Integer pageSize);

    /**
     * 会话列表
     */
    List<ChatSessionVo> queryChatSessions(Long myId);

    /**
     * 总未读数
     */
    Long queryTotalUnread(Long myId);

    /**
     * 标记已读（与某用户会话，id<=upToMsgId；upToMsgId 为空则全部标记）
     *
     * @return 本次更新条数
     */
    int markChatRead(Long myId, Long withUserId, Long upToMsgId);
}
