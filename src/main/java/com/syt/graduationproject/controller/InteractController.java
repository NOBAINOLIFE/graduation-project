package com.syt.graduationproject.controller;

import com.syt.graduationproject.exception.CustomException;
import com.syt.graduationproject.model.request.CommentRequest;
import com.syt.graduationproject.model.request.FollowRequest;
import com.syt.graduationproject.model.request.LikeRequest;
import com.syt.graduationproject.model.response.Response;
import com.syt.graduationproject.model.vo.ChatSessionVo;
import com.syt.graduationproject.model.vo.PrivateMessageVo;
import com.syt.graduationproject.service.InteractService;
import com.syt.graduationproject.util.UserHolderUtil;
import com.syt.graduationproject.websocket.ChatWsConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 交互控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/graduation-project/interact")
public class InteractController {

    private final InteractService interactService;

    /**
     * 获取 WebSocket 私聊端点（前端使用 ws://host:port + 返回的 path，并带上 ?token=xxx）
     */
    @GetMapping("/chat/wsPath")
    public Response<String> wsPath() {
        return Response.success(ChatWsConstants.WS_PATH);
    }

    /**
     * 会话列表
     */
    @GetMapping("/chat/sessions")
    public Response<List<ChatSessionVo>> sessions() {
        Long myId = UserHolderUtil.getUser().getUserId();
        return Response.success(interactService.queryChatSessions(myId));
    }

    /**
     * 拉取历史消息（倒序返回）
     *
     * @param withUserId 对方用户ID
     * @param beforeId 游标（传上一页最小消息id，用于翻更老；不传则取最新）
     * @param pageSize 每页条数（默认20，最大100）
     */
    @GetMapping("/chat/history")
    public Response<List<PrivateMessageVo>> history(@RequestParam("withUserId") Long withUserId,
                                                    @RequestParam(value = "beforeId", required = false) Long beforeId,
                                                    @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        Long myId = UserHolderUtil.getUser().getUserId();
        return Response.success(interactService.queryChatHistory(myId, withUserId, beforeId, pageSize));
    }

    /**
     * 总未读数
     */
    @GetMapping("/chat/unread/total")
    public Response<Long> totalUnread() {
        Long myId = UserHolderUtil.getUser().getUserId();
        return Response.success(interactService.queryTotalUnread(myId));
    }

    /**
     * 标记已读（与某用户）
     *
     * @param withUserId 对方用户ID
     * @param upToMsgId 读到的消息ID（为空则全部标记已读）
     */
    @PostMapping("/chat/read")
    public Response<Integer> markRead(@RequestParam("withUserId") Long withUserId,
                                      @RequestParam(value = "upToMsgId", required = false) Long upToMsgId) {
        Long myId = UserHolderUtil.getUser().getUserId();
        return Response.success(interactService.markChatRead(myId, withUserId, upToMsgId));
    }

    /**
     * 关注/取关用户
     */
    @PostMapping("/follow")
    public Response<Object> follow(@RequestBody FollowRequest request) {
        try {
            interactService.follow(request);
            return Response.success();
        } catch (CustomException e) {
            log.warn("用户关注失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("用户关注失败，followRequest：{}", request, e);
            return Response.fail("关注失败，系统异常");
        }
    }

    /**
     * 点赞/取消点赞视频
     */
    @PostMapping("/likeVideo")
    public Response<Object> likeVideo(@RequestBody LikeRequest request) {
        try {
            interactService.likeVideo(request);
            return Response.success();
        } catch (CustomException e) {
            log.warn("用户点赞视频失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("用户点赞失败，likeRequest：{}", request, e);
            return Response.fail("点赞失败，系统异常");
        }
    }

    /**
     * 评论/回复评论
     */
    @PostMapping("/comment")
    public Response<Object> comment(@RequestBody CommentRequest request) {
        try {
            interactService.comment(request);
            return Response.success();
        } catch (CustomException e) {
            log.warn("用户评论失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("用户评论失败，commentRequest：{}", request, e);
            return Response.fail("评论失败，系统异常");
        }
    }

    /**
     * 点赞/取消点赞评论
     */
    @PostMapping("/likeComment")
    public Response<Object> likeComment(@RequestBody LikeRequest request) {
        try {
            interactService.likeComment(request);
            return Response.success();
        } catch (CustomException e) {
            log.warn("用户点赞评论失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("用户点赞失败，likeRequest：{}", request, e);
            return Response.fail("点赞失败，系统异常");
        }
    }
}
