package com.syt.graduationproject.controller;

import com.syt.graduationproject.constant.ChatWsConstant;
import com.syt.graduationproject.exception.CustomException;
import com.syt.graduationproject.model.request.*;
import com.syt.graduationproject.model.response.Response;
import com.syt.graduationproject.model.vo.*;
import com.syt.graduationproject.service.InteractService;
import com.syt.graduationproject.util.UserHolderUtil;
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
        return Response.success(ChatWsConstant.WS_PATH);
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
     * @param beforeId   游标（传上一页最小消息id，用于翻更老；不传则取最新）
     * @param pageSize   每页条数（默认20，最大100）
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
     * @param upToMsgId  读到的消息ID（为空则全部标记已读）
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
     * 拉黑/取消拉黑用户
     */
    @PostMapping("/block")
    public Response<Object> block(@RequestBody BlockUserRequest request) {
        try {
            interactService.blockUser(request);
            return Response.success();
        } catch (CustomException e) {
            log.warn("用户拉黑操作失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("用户拉黑操作失败，request：{}", request, e);
            return Response.fail("拉黑操作失败，系统异常");
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

    /**
     * 查询评论列表
     */
    @GetMapping("/comment/list")
    public Response<PageVo<CommentVo>> queryCommentList(@RequestParam("videoId") Long videoId,
                                                        @RequestParam(value = "sortType") Integer sortType,
                                                        @RequestParam(value = "pageNum", required = false) Integer pageNum,
                                                        @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        try {
            return Response.success(interactService.listVideoComments(videoId, sortType, pageNum, pageSize));
        } catch (CustomException e) {
            log.warn("查询评论列表失败，videoId：{}，原因：{}", videoId, e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询评论列表失败，videoId：{}", videoId, e);
            return Response.fail("查询评论列表失败，系统异常");
        }
    }

    /**
     * 查询粉丝列表
     */
    @PostMapping("/fansList")
    public Response<List<UserSimpleInfoVo>> queryFansList() {
        Long userId = UserHolderUtil.getUser().getUserId();
        try {
            return Response.success(interactService.queryFansList(userId));
        } catch (CustomException e) {
            log.warn("查询粉丝列表失败，userId：{}，原因：{}", userId, e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询粉丝列表失败，userId：{}", userId, e);
            return Response.fail("查询粉丝列表失败，系统异常");
        }
    }

    /**
     * 查询关注列表
     */
    @PostMapping("/followList")
    public Response<List<UserSimpleInfoVo>> queryFollowList() {
        Long userId = UserHolderUtil.getUser().getUserId();
        try {
            return Response.success(interactService.queryFollowList(userId));
        } catch (CustomException e) {
            log.warn("查询关注列表失败，userId：{}，原因：{}", userId, e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询关注列表失败，userId：{}", userId, e);
            return Response.fail("查询关注列表失败，系统异常");
        }
    }

    /**
     * 收藏视频
     */
    @PostMapping("/collectVideo")
    public Response<Object> collectVideo(@RequestBody CollectVideoRequest request) {
        try {
            interactService.collectVideo(request);
            return Response.success();
        } catch (CustomException e) {
            log.warn("用户收藏视频失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("用户收藏视频失败，likeRequest：{}", request, e);
            return Response.fail("收藏失败，系统异常");
        }
    }

    /**
     * 查询视频与收藏夹关系
     */
    @GetMapping("/collection/rel")
    public Response<List<VideoDirectoryRelationVo>> queryVideoDirectoryRelations(@RequestParam("videoId") Long videoId) {
        try {
            return Response.success(interactService.queryVideoDirectoryRelations(videoId));
        } catch (CustomException e) {
            log.warn("查询视频收藏关系失败，videoId：{}，原因：{}", videoId, e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询视频收藏关系失败，videoI：{}", videoId, e);
            return Response.fail("查询视频收藏关系失败，系统异常");
        }
    }

    /**
     * 创建收藏夹
     */
    @PostMapping("/collection/directory/create")
    public Response<Long> createCollectionDirectory(@RequestBody CollectionDirectoryCreateRequest request) {
        try {
            return Response.success(interactService.createCollectionDirectory(request));
        } catch (CustomException e) {
            log.warn("创建收藏夹失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("创建收藏夹失败，request：{}", request, e);
            return Response.fail("创建收藏夹失败，系统异常");
        }
    }

    /**
     * 编辑收藏夹
     */
    @PostMapping("/collection/directory/update")
    public Response<Object> updateCollectionDirectory(@RequestBody CollectionDirectoryUpdateRequest request) {
        try {
            interactService.updateCollectionDirectory(request);
            return Response.success();
        } catch (CustomException e) {
            log.warn("编辑收藏夹失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("编辑收藏夹失败，request：{}", request, e);
            return Response.fail("编辑收藏夹失败，系统异常");
        }
    }

    /**
     * 删除收藏夹
     */
    @GetMapping("/collection/directory/delete")
    public Response<Object> deleteCollectionDirectory(@RequestParam("directoryId") Long directoryId) {
        try {
            interactService.deleteCollectionDirectory(directoryId);
            return Response.success();
        } catch (CustomException e) {
            log.warn("删除收藏夹失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("删除收藏夹失败，directoryId：{}", directoryId, e);
            return Response.fail("删除收藏夹失败，系统异常");
        }
    }

    /**
     * 收藏夹列表（targetUserId 不传时查本人）
     */
    @GetMapping("/collection/directory/list")
    public Response<List<CollectionDirectoryVo>> listCollectionDirectories(@RequestParam(value = "targetUserId", required = false) Long targetUserId) {
        try {
            return Response.success(interactService.listCollectionDirectories(targetUserId));
        } catch (CustomException e) {
            log.warn("查询收藏夹列表失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询收藏夹列表失败，targetUserId：{}", targetUserId, e);
            return Response.fail("查询收藏夹列表失败，系统异常");
        }
    }

    /**
     * 收藏夹批量操作：取消收藏/复制/移动
     */
    @PostMapping("/collection/item/batch")
    public Response<Integer> batchOperateCollectionItems(@RequestBody CollectionBatchOperateRequest request) {
        try {
            return Response.success(interactService.batchOperateCollectionItems(request));
        } catch (CustomException e) {
            log.warn("收藏夹批量操作失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("收藏夹批量操作失败，request：{}", request, e);
            return Response.fail("收藏夹批量操作失败，系统异常");
        }
    }

    /**
     * 一键清除收藏夹失效内容
     */
    @PostMapping("/collection/item/clearInvalid")
    public Response<Integer> clearInvalidCollectionItems(@RequestParam("directoryId") Long directoryId) {
        try {
            return Response.success(interactService.clearInvalidCollectionItems(directoryId));
        } catch (CustomException e) {
            log.warn("清除失效收藏失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("清除失效收藏失败，directoryId：{}", directoryId, e);
            return Response.fail("清除失效收藏失败，系统异常");
        }
    }

    /**
     * 给视频投币
     */
    @PostMapping("/coin")
    public Response<Object> coinVideo(@RequestBody CoinVideoRequest request) {
        try {
            interactService.coinVideo(request);
            return Response.success();
        } catch (CustomException e) {
            log.warn("视频投币失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("视频投币失败，request：{}", request, e);
            return Response.fail("视频投币失败，系统异常");
        }
    }

    /**
     * 查询我的硬币钱包
     */
    @GetMapping("/coin/wallet")
    public Response<CoinWalletVo> queryMyWallet() {
        try {
            return Response.success(interactService.queryMyWallet());
        } catch (CustomException e) {
            log.warn("查询硬币钱包失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询硬币钱包失败", e);
            return Response.fail("查询硬币钱包失败，系统异常");
        }
    }

    /**
     * 一键三连：点赞 + 投币1个 + 加入默认收藏夹
     */
    @PostMapping("/tripleAction")
    public Response<Object> tripleAction(@RequestBody TripleActionRequest request) {
        try {
            interactService.tripleAction(request);
            return Response.success();
        } catch (CustomException e) {
            log.warn("一键三连失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("一键三连失败，request：{}", request, e);
            return Response.fail("一键三连失败，系统异常");
        }
    }

    /**
     * 举报用户或视频
     */
    @PostMapping("/report")
    public Response<Object> submitReport(@RequestBody ReportSubmitRequest request) {
        try {
            interactService.submitReport(request);
            return Response.success();
        } catch (CustomException e) {
            log.warn("提交举报失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("提交举报失败，request：{}", request, e);
            return Response.fail("提交举报失败，系统异常");
        }
    }

    /**
     * 查询当前用户的举报信息
     */
    @GetMapping("/report/my")
    public Response<List<ReportVo>> listMyReports() {
        try {
            return Response.success(interactService.listMyReports());
        } catch (CustomException e) {
            log.warn("查询举报信息失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询举报信息失败", e);
            return Response.fail("查询举报信息失败，系统异常");
        }
    }

    /**
     * 查询收藏夹内视频
     */
    @GetMapping("/collection/item/list")
    public Response<List<SearchVideoVo>> listCollectionItems(
            @RequestParam("directoryId") Long directoryId,
            @RequestParam(value = "sortType", defaultValue = "1") Integer sortType) {
        try {
            return Response.success(interactService.listCollectionItems(directoryId, sortType));
        } catch (CustomException e) {
            log.warn("查询收藏夹内视频，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询收藏夹内视频失败", e);
            return Response.fail("查询收藏夹内视频失败，系统异常");
        }
    }
}
