package com.syt.graduationproject.controller;

import com.syt.graduationproject.annotation.RequirePermission;
import com.syt.graduationproject.exception.CustomException;
import com.syt.graduationproject.model.request.LoginRequest;
import com.syt.graduationproject.model.request.ManagerAuditVideoListRequest;
import com.syt.graduationproject.model.request.ManagerAuditVideoRequest;
import com.syt.graduationproject.model.request.ManagerReportListRequest;
import com.syt.graduationproject.model.request.ManagerReviewReportRequest;
import com.syt.graduationproject.model.request.ManagerUserListRequest;
import com.syt.graduationproject.model.request.ManagerVideoPartitionListRequest;
import com.syt.graduationproject.model.request.ManagerVideoTagListRequest;
import com.syt.graduationproject.model.response.Response;
import com.syt.graduationproject.model.vo.LoginVo;
import com.syt.graduationproject.model.vo.ManagerVideoPartitionVo;
import com.syt.graduationproject.model.vo.ManagerUserVo;
import com.syt.graduationproject.model.vo.ManagerVideoTagVo;
import com.syt.graduationproject.model.vo.VideoAuditVo;
import com.syt.graduationproject.model.vo.report.ManagerReportRecordVo;
import com.syt.graduationproject.model.vo.Page.PageVo;
import com.syt.graduationproject.service.ManagerService;
import com.syt.graduationproject.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.syt.graduationproject.constant.UserConstant.ADMIN_PERMISSION;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/graduation-project/manager")
public class ManagerController {

    private final ManagerService managerService;

    private final UserService userService;

    /**
     * 管理员登录（无需权限校验）
     */
    @PostMapping("/login")
    public Response<LoginVo> login(@RequestBody LoginRequest request) {
        try {
            return Response.success(userService.login(request));
        } catch (CustomException e) {
            log.warn("管理员登录失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("管理员登录失败，loginRequest：{}", request, e);
            return Response.fail("管理员登录失败，系统异常");
        }
    }

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

    @PostMapping("/user/list")
    @RequirePermission(ADMIN_PERMISSION)
    public Response<PageVo<ManagerUserVo>> queryUserList(@RequestBody(required = false) ManagerUserListRequest request) {
        try {
            return Response.success(managerService.queryUserList(request));
        } catch (CustomException e) {
            log.warn("查询用户列表失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询用户列表失败，request:{}", request, e);
            return Response.fail("查询用户列表失败，系统异常");
        }
    }

    @PostMapping("/video/partition/list")
    @RequirePermission(ADMIN_PERMISSION)
    public Response<PageVo<ManagerVideoPartitionVo>> queryVideoPartitionList(@RequestBody(required = false) ManagerVideoPartitionListRequest request) {
        try {
            return Response.success(managerService.queryVideoPartitionList(request));
        } catch (CustomException e) {
            log.warn("查询视频分区列表失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询视频分区列表失败，request:{}", request, e);
            return Response.fail("查询视频分区列表失败，系统异常");
        }
    }

    @PostMapping("/video/tag/list")
    @RequirePermission(ADMIN_PERMISSION)
    public Response<PageVo<ManagerVideoTagVo>> queryVideoTagList(@RequestBody(required = false) ManagerVideoTagListRequest request) {
        try {
            return Response.success(managerService.queryVideoTagList(request));
        } catch (CustomException e) {
            log.warn("查询视频标签列表失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询视频标签列表失败，request:{}", request, e);
            return Response.fail("查询视频标签列表失败，系统异常");
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

    @PostMapping("/video/partition/create")
    @RequirePermission(ADMIN_PERMISSION)
    public Response<Object> createVideoPartition(@RequestBody Map<String, String> body) {
        try {
            managerService.createVideoPartition(body.get("partitionName"));
            return Response.success();
        } catch (CustomException e) {
            log.warn("创建视频分区失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("创建视频分区失败", e);
            return Response.fail("创建视频分区失败，系统异常");
        }
    }

    @DeleteMapping("/video/partition/{partitionId}")
    @RequirePermission(ADMIN_PERMISSION)
    public Response<Object> deleteVideoPartition(@PathVariable Long partitionId) {
        try {
            managerService.deleteVideoPartition(partitionId);
            return Response.success();
        } catch (CustomException e) {
            log.warn("删除视频分区失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("删除视频分区失败，partitionId:{}", partitionId, e);
            return Response.fail("删除视频分区失败，系统异常");
        }
    }

    @DeleteMapping("/video/tag/{tagId}")
    @RequirePermission(ADMIN_PERMISSION)
    public Response<Object> deleteVideoTag(@PathVariable Long tagId) {
        try {
            managerService.deleteVideoTag(tagId);
            return Response.success();
        } catch (CustomException e) {
            log.warn("删除视频标签失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("删除视频标签失败，tagId:{}", tagId, e);
            return Response.fail("删除视频标签失败，系统异常");
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

    @PostMapping("/comment/delete/{commentId}")
    @RequirePermission(ADMIN_PERMISSION)
    public Response<Object> deleteComment(@PathVariable Long commentId) {
        try {
            managerService.deleteComment(commentId);
            return Response.success();
        } catch (CustomException e) {
            log.warn("删除评论失败，commentId：{}，原因：{}", commentId, e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("删除评论失败，commentId：{}", commentId, e);
            return Response.fail("删除评论失败，系统异常");
        }
    }
}

