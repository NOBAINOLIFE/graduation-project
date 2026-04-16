package com.syt.graduationproject.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultipartUploadSessionDto {

    private Long videoId;

    private Long userId;

    private String uploadToken;

    private String fileName;

    private String contentType;

    private Integer totalChunks;

    private LocalDateTime createdAt;
}

