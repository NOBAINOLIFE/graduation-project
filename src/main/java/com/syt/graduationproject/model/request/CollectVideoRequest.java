package com.syt.graduationproject.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectVideoRequest {

    private Long videoId;

    private List<Long> collectDirectoryIdList;

    private List<Long> removeDirectoryIdList;
}
