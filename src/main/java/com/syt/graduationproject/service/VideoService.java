package com.syt.graduationproject.service;

import com.syt.graduationproject.model.request.VideoUploadRequest;
import com.syt.graduationproject.model.vo.VideoPlayDetailVo;
import com.syt.graduationproject.model.vo.VideoUploadConfirmVo;
import com.syt.graduationproject.model.vo.VideoUploadVo;
import org.springframework.web.multipart.MultipartFile;

public interface VideoService {

    /**
     * 查询用户视频数
     */
    Long queryVideoNum(Long userId);

    /**
     * 查询播放页视频详情
     */
    VideoPlayDetailVo queryVideoInfo(Long videoId);

    /**
     * 获取视频上传链接
     */
    VideoUploadVo askForUpload(VideoUploadRequest request);

    /**
     * 确认视频上传
     */
    VideoUploadConfirmVo confirmUpload(Long videoId);
}
