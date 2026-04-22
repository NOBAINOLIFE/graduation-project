package com.syt.graduationproject.controller;

import com.syt.graduationproject.exception.CustomException;
import com.syt.graduationproject.model.request.VideoPlayProgressRequest;
import com.syt.graduationproject.model.request.VideoSubmitRequest;
import com.syt.graduationproject.model.response.Response;
import com.syt.graduationproject.model.vo.VideoPlayDetailVo;
import com.syt.graduationproject.model.vo.VideoPartitionVo;
import com.syt.graduationproject.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public Response<VideoPlayDetailVo> queryVideoInfo(@PathVariable Long videoId) {
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

    /**
     * 投稿（填写标题/简介/封面）
     */
    @PostMapping("/submit")
    public Response<Object> submitVideo(@RequestBody VideoSubmitRequest request) {
        try {
            videoService.submitVideo(request);
            return Response.success();
        } catch (CustomException e) {
            log.warn("视频投稿失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("视频投稿失败，request:{}", request, e);
            return Response.fail("视频投稿失败，系统异常");
        }
    }

    /**
     * 发布视频
     */
    @PostMapping("/publish/{videoId}")
    public Response<Object> publishVideo(@PathVariable Long videoId) {
        try {
            videoService.publishVideo(videoId);
            return Response.success();
        } catch (CustomException e) {
            log.warn("发布视频失败，videoId:{}, 原因:{}", videoId, e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("发布视频失败，videoId:{}", videoId, e);
            return Response.fail("发布视频失败，系统异常");
        }
    }

    /**
     * 上报视频播放进度
     */
    @PostMapping("/play/progress")
    public Response<Object> reportPlayProgress(@RequestBody VideoPlayProgressRequest request) {
        try {
            videoService.reportPlayProgress(request);
            return Response.success();
        } catch (CustomException e) {
            log.warn("上报视频播放进度失败，原因:{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("上报视频播放进度失败，request:{}", request, e);
            return Response.fail("上报视频播放进度失败，系统异常");
        }
    }

    /**
     * 查询视频分区列表
     */
    @GetMapping("/partitions")
    public Response<List<VideoPartitionVo>> listPartitions() {
        try {
            return Response.success(videoService.listAllPartitions());
        } catch (CustomException e) {
            log.warn("查询视频分区列表失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询视频分区列表失败", e);
            return Response.fail("查询视频分区列表失败，系统异常");
        }
    }
}
