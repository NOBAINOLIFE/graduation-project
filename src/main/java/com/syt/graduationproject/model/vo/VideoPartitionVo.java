package com.syt.graduationproject.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 视频分区VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoPartitionVo {

    private Long id;

    private String partitionName;
}

