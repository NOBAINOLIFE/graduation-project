package com.syt.graduationproject.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageVo<T> {

    private Long total;

    private Integer pageNum;

    private Integer pageSize;

    private List<T> records;
}

