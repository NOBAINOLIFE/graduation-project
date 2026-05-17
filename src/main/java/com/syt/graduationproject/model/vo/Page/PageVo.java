package com.syt.graduationproject.model.vo.Page;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PageVo<T> {

    private Long total;

    private Integer pageNum;

    private Integer pageSize;

    private Boolean isEnd;

    private List<T> records;
}

