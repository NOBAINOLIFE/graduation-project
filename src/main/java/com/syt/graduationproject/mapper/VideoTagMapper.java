package com.syt.graduationproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syt.graduationproject.model.po.VideoTagPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface VideoTagMapper extends BaseMapper<VideoTagPo> {

    int increaseHotBatch(@Param("tagIds") List<Long> tagIds);
}

