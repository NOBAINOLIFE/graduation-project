package com.syt.graduationproject.controller;

import com.syt.graduationproject.exception.CustomException;
import com.syt.graduationproject.model.request.FollowRequest;
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
            return Response.fail("注册失败，系统异常");
        }
    }
}
