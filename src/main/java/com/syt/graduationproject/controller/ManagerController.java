package com.syt.graduationproject.controller;

import com.syt.graduationproject.annotation.RequirePermission;
import com.syt.graduationproject.exception.CustomException;
import com.syt.graduationproject.model.request.ManagerAuditVideoListRequest;
import com.syt.graduationproject.model.request.ManagerAuditVideoRequest;
import com.syt.graduationproject.model.request.ManagerReportListRequest;
import com.syt.graduationproject.model.request.ManagerReviewReportRequest;
import com.syt.graduationproject.model.response.Response;
import com.syt.graduationproject.model.vo.VideoAuditVo;
import com.syt.graduationproject.model.vo.report.ManagerReportRecordVo;
import com.syt.graduationproject.model.vo.PageVo;
import com.syt.graduationproject.service.ManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import static com.syt.graduationproject.constant.UserConstant.ADMIN_PERMISSION;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/graduation-project/manager")
public class ManagerController {

    private final ManagerService managerService;

    @PostMapping("/video/audit/list")
    @RequirePermission(ADMIN_PERMISSION)
    public Response<PageVo<VideoAuditVo>> queryAuditVideoList(@RequestBody ManagerAuditVideoListRequest request) {
        try {
            return Response.success(managerService.queryAuditVideoList(request));
        } catch (CustomException e) {
            log.warn("查询视频审核列表失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询视频审核列表失败，request:{}", request, e);
            return Response.fail("查询视频审核列表失败，系统异常");
        }
    }

    @PostMapping("/report/list")
    @RequirePermission(ADMIN_PERMISSION)
    public Response<PageVo<ManagerReportRecordVo>> queryReportList(@RequestBody ManagerReportListRequest request) {
        try {
            return Response.success(managerService.queryReportList(request));
        } catch (CustomException e) {
            log.warn("查询举报列表失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询举报列表失败，request:{}", request, e);
            return Response.fail("查询举报列表失败，系统异常");
        }
    }

    @PostMapping("/report/review")
    @RequirePermission(ADMIN_PERMISSION)
    public Response<Object> reviewReport(@RequestBody ManagerReviewReportRequest request) {
        try {
            managerService.reviewReport(request);
            return Response.success();
        } catch (CustomException e) {
            log.warn("审核举报失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("审核举报失败，request:{}", request, e);
            return Response.fail("审核举报失败，系统异常");
        }
    }

    @PostMapping("/user/ban/{userId}")
    @RequirePermission(ADMIN_PERMISSION)
    public Response<Object> banUser(@PathVariable Long userId) {
        try {
            managerService.banUser(userId);
            return Response.success();
        } catch (CustomException e) {
            log.warn("封禁用户失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("封禁用户失败，userId:{}", userId, e);
            return Response.fail("封禁用户失败，系统异常");
        }
    }

    @PostMapping("/user/unban/{userId}")
    @RequirePermission(ADMIN_PERMISSION)
    public Response<Object> unbanUser(@PathVariable Long userId) {
        try {
            managerService.unbanUser(userId);
            return Response.success();
        } catch (CustomException e) {
            log.warn("解禁用户失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("解禁用户失败，userId:{}", userId, e);
            return Response.fail("解禁用户失败，系统异常");
        }
    }

    @PostMapping("/video/ban/{videoId}")
    @RequirePermission(ADMIN_PERMISSION)
    public Response<Object> banVideo(@PathVariable Long videoId) {
        try {
            managerService.banVideo(videoId);
            return Response.success();
        } catch (CustomException e) {
            log.warn("封禁视频失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("封禁视频失败，videoId:{}", videoId, e);
            return Response.fail("封禁视频失败，系统异常");
        }
    }

    @PostMapping("/video/unban/{videoId}")
    @RequirePermission(ADMIN_PERMISSION)
    public Response<Object> unbanVideo(@PathVariable Long videoId) {
        try {
            managerService.unbanVideo(videoId);
            return Response.success();
        } catch (CustomException e) {
            log.warn("解禁视频失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("解禁视频失败，videoId:{}", videoId, e);
            return Response.fail("解禁视频失败，系统异常");
        }
    }

    @PostMapping("/video/audit")
    @RequirePermission(ADMIN_PERMISSION)
    public Response<Object> auditVideo(@RequestBody ManagerAuditVideoRequest request) {
        try {
            managerService.auditVideo(request);
            return Response.success();
        } catch (CustomException e) {
            log.warn("审核视频失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("审核视频失败，request:{}", request, e);
            return Response.fail("审核视频失败，系统异常");
        }
    }
}

