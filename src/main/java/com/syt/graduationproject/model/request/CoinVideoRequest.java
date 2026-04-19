package com.syt.graduationproject.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoinVideoRequest {

    private Long videoId;

    /**
     * 允许 1 或 2
     */
    private Integer amount;
}

