package com.syt.graduationproject.controller;

import com.syt.graduationproject.exception.CustomException;
import com.syt.graduationproject.model.request.CommentRequest;
import com.syt.graduationproject.model.request.FollowRequest;
import com.syt.graduationproject.model.request.LikeRequest;
import com.syt.graduationproject.model.response.Response;
import com.syt.graduationproject.service.InteractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
            log.warn("用户点赞失败，原因：{}", e.getMessage());
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
            log.warn("用户点赞失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("用户点赞失败，likeRequest：{}", request, e);
            return Response.fail("点赞失败，系统异常");
        }
    }
}
