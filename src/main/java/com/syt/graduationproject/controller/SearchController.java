package com.syt.graduationproject.controller;

import com.syt.graduationproject.exception.CustomException;
import com.syt.graduationproject.model.request.SearchUserRequest;
import com.syt.graduationproject.model.request.SearchVideoRequest;
import com.syt.graduationproject.model.response.Response;
import com.syt.graduationproject.model.vo.PageVo;
import com.syt.graduationproject.model.vo.SearchUserVo;
import com.syt.graduationproject.model.vo.SearchVideoVo;
import com.syt.graduationproject.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/graduation-project/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * 视频检索
     */
    @PostMapping("/video")
    public Response<PageVo<SearchVideoVo>> searchVideos(@RequestBody SearchVideoRequest request) {
        try {
            return Response.success(searchService.searchVideos(request));
        } catch (CustomException e) {
            log.warn("视频检索失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("视频检索失败，request：{}", request, e);
            return Response.fail("视频检索失败，系统异常");
        }
    }

    /**
     * 用户检索
     */
    @PostMapping("/user")
    public Response<PageVo<SearchUserVo>> searchUsers(@RequestBody SearchUserRequest request) {
        try {
            return Response.success(searchService.searchUsers(request));
        } catch (CustomException e) {
            log.warn("用户检索失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("用户检索失败，request：{}", request, e);
            return Response.fail("用户检索失败，系统异常");
        }
    }
}

