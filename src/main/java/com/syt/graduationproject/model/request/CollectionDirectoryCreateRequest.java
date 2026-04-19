package com.syt.graduationproject.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionDirectoryCreateRequest {

    private String name;

    private String description;

    private String coverUrl;

    /**
     * 1: 公开, 0: 私有
     */
    private Integer isPublic;
}

