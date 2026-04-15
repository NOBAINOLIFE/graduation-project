package com.syt.graduationproject.controller;

import com.syt.graduationproject.model.response.Response;
import com.syt.graduationproject.service.minio.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/graduation-project/upload")
@RequiredArgsConstructor
public class UploadController {

    private final MinioService minioService;

    /**
     * 上传图片
     */
    @PostMapping("/image")
    public Response<String> uploadImage(MultipartFile file) {
        try {
            return Response.success(minioService.uploadFile(file, "cover/"));
        } catch (Exception e) {
            log.error("上传图片失败，filename", e);
            return Response.fail("上传图片失败，系统异常");
        }
    }
}
