package com.syt.graduationproject.controller;

import com.syt.graduationproject.annotation.RequirePermission;
import com.syt.graduationproject.client.MyMinioClient;
import com.syt.graduationproject.exception.CustomException;
import com.syt.graduationproject.model.request.MultipartUploadCompleteRequest;
import com.syt.graduationproject.model.request.MultipartUploadInitRequest;
import com.syt.graduationproject.model.response.Response;
import com.syt.graduationproject.model.vo.MultipartUploadInitVo;
import com.syt.graduationproject.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/graduation-project/upload")
@RequiredArgsConstructor
@RequirePermission("USER")
public class UploadController {

    private final MyMinioClient myMinioClient;

    private final UploadService uploadService;

    /**
     * 上传图片
     */
    @PostMapping("/image")
    public Response<String> uploadImage(MultipartFile file) {
        try {
            return Response.success(myMinioClient.uploadFile(file, "cover/"));
        } catch (Exception e) {
            log.error("上传图片失败，filename", e);
            return Response.fail("上传图片失败，系统异常");
        }
    }

    /**
     * 初始化分片上传
     */
    @PostMapping("/multipart/new")
    public Response<MultipartUploadInitVo> initMultipartUpload(@RequestBody MultipartUploadInitRequest request) {
        try {
            return Response.success(uploadService.initMultipartUpload(request));
        } catch (CustomException e) {
            log.warn("初始化分片上传失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("初始化分片上传失败，request:{}", request, e);
            return Response.fail("初始化分片上传失败，系统异常");
        }
    }

    /**
     * 上传分片
     */
    @PostMapping("/multipart/part")
    public Response<Object> uploadPart(@RequestParam("videoId") Long videoId,
                                       @RequestParam("uploadToken") String uploadToken,
                                       @RequestParam("chunkIndex") Integer chunkIndex,
                                       @RequestParam("file") MultipartFile file) {
        try {
            uploadService.uploadMultipartChunk(videoId, uploadToken, chunkIndex, file);
            return Response.success();
        } catch (CustomException e) {
            log.warn("上传分片失败，videoId:{}, chunkIndex:{}, 原因:{}", videoId, chunkIndex, e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("上传分片失败，videoId:{}, chunkIndex:{}", videoId, chunkIndex, e);
            return Response.fail("上传分片失败，系统异常");
        }
    }

    /**
     * 完成分片上传
     */
    @PostMapping("/multipart/complete")
    public Response<Object> completeMultipartUpload(@RequestBody MultipartUploadCompleteRequest request) {
        try {
            uploadService.completeMultipartUpload(request);
            return Response.success();
        } catch (CustomException e) {
            log.warn("完成分片上传失败，原因：{}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("完成分片上传失败，request:{}", request, e);
            return Response.fail("完成分片上传失败，系统异常");
        }
    }
}
