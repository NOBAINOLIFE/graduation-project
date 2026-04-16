package com.syt.graduationproject.service;

import com.syt.graduationproject.model.request.MultipartUploadCompleteRequest;
import com.syt.graduationproject.model.request.MultipartUploadInitRequest;
import com.syt.graduationproject.model.vo.MultipartUploadInitVo;
import org.springframework.web.multipart.MultipartFile;

public interface UploadService {

    MultipartUploadInitVo initMultipartUpload(MultipartUploadInitRequest request);

    void uploadMultipartChunk(Long videoId, String uploadToken, Integer chunkIndex, MultipartFile file);

    void completeMultipartUpload(MultipartUploadCompleteRequest request);
}

