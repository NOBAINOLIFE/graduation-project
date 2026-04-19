package com.syt.graduationproject.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionDirectoryVo {

    private Long directoryId;

    private String name;

    private String description;

    private String coverUrl;

    private Integer isPublic;

    private Boolean isDefault;

    private Long itemCount;
}

