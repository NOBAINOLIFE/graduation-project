package com.syt.graduationproject.controller;

import com.syt.graduationproject.exception.CustomException;
import com.syt.graduationproject.model.response.Response;
import com.syt.graduationproject.model.vo.VideoPlayDetailVo;
import com.syt.graduationproject.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 视频控制器
 */
@Slf4j
@RestController
@RequestMapping("/graduation-project/video")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    /**
     * 播放页视频详情
     */
    @GetMapping("/videoInfo/{videoId}")
    public Response<VideoPlayDetailVo> queryVideoInfo(@PathVariable("videoId") Long videoId) {
        try {
            return Response.success(videoService.queryVideoInfo(videoId));
        } catch (CustomException e) {
            log.warn("查询视频详情失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询视频详情失败，videoId：{}", videoId, e);
            return Response.fail("查询视频详情失败，系统异常");
        }
    }
}
