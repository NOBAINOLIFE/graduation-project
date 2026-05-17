package com.syt.graduationproject.service;

import com.syt.graduationproject.model.request.*;
import com.syt.graduationproject.model.vo.*;
import com.syt.graduationproject.model.vo.Page.CommentPageVo;
import com.syt.graduationproject.model.vo.Page.PageVo;
import com.syt.graduationproject.model.websocket.PrivateChatSendRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

public interface InteractService {

    /**
     * 关注/取关
     */
    @Transactional(rollbackFor = Exception.class)
    void follow(FollowRequest request);

    @Transactional(rollbackFor = Exception.class)
    void blockUser(BlockUserRequest request);

    List<UserSimpleInfoVo> queryMyBlockList();

    boolean hasMutualBlock(Long userA, Long userB);

    /**
     * 评论/回复评论
     */
    @Transactional(rollbackFor = Exception.class)
    void comment(CommentRequest request);

    @Transactional(rollbackFor = Exception.class)
    void deleteComment(Long commentId);

    @Transactional(rollbackFor = Exception.class)
    void topComment(CommentTopRequest request);

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
     * WebSocket：接收方消息确认（收到 chat 后回 chat_recv_ack）
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

    List<ReplyMessageVo> queryReplyMessages();

    List<LikeReceivedSummaryVo> queryLikeReceivedSummaries();

    LikeReceivedDetailVo queryLikeReceivedDetail(String targetType, Long targetId);

    List<UserSimpleInfoVo> queryFansList(Long userId);

    List<UserSimpleInfoVo> queryFollowList(Long userId);

    void collectVideo(CollectVideoRequest request);

    @Transactional(rollbackFor = Exception.class)
    Long createCollectionDirectory(CollectionDirectoryCreateRequest request);

    @Transactional(rollbackFor = Exception.class)
    void updateCollectionDirectory(CollectionDirectoryUpdateRequest request);

    List<CollectionDirectoryVo> listCollectionDirectories(Long targetUserId);

    List<VideoDirectoryRelationVo> queryVideoDirectoryRelations(Long videoId);

    @Transactional(rollbackFor = Exception.class)
    Integer batchOperateCollectionItems(CollectionBatchOperateRequest request);

    @Transactional(rollbackFor = Exception.class)
    Integer clearInvalidCollectionItems(Long directoryId);

    @Transactional(rollbackFor = Exception.class)
    void coinVideo(CoinVideoRequest request);

    @Transactional(rollbackFor = Exception.class)
    boolean claimDailyCoin(Long userId);

    CoinWalletVo queryMyWallet();

    @Transactional(rollbackFor = Exception.class)
    void tripleAction(TripleActionRequest request);

    boolean shareVideo(Long videoId);

    void submitReport(ReportSubmitRequest request);

    List<SearchVideoVo> listCollectionItems(Long directoryId, Integer sortType);

    void deleteCollectionDirectory(Long directoryId);

    CommentPageVo listVideoComments(CommentListRequest request);

    PageVo<CommentVo> listCommentReplies(Long rootCommentId, Integer pageNum, Integer pageSize);

    PageVo<CreatorCommentManageVo> listCreatorComments(CreatorCommentQueryRequest request);

    @Transactional(rollbackFor = Exception.class)
    void deleteCreatorComment(Long commentId);
}
