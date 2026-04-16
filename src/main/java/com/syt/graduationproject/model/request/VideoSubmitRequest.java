package com.syt.graduationproject.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoSubmitRequest {

    private Long videoId;

    private String title;

    private String description;

    private String coverUrl;

    private Integer duration;
}

