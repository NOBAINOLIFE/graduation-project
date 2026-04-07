package com.syt.graduationproject.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoTranscodingDto {

    private Long videoId;

    private String originalResolutionUrl;
}
