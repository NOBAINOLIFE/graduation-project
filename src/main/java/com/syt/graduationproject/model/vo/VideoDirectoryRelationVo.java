package com.syt.graduationproject.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoDirectoryRelationVo {

    private Long videoId;

    private Long directoryId;

    private String directoryName;

    private Boolean isPublic;

    private Boolean isDefault;

    private Boolean isCollect;
}
