package com.syt.graduationproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syt.graduationproject.model.po.VideoPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 视频信息表 Mapper 接口
 */
@Mapper
public interface VideoMapper extends BaseMapper<VideoPo> {
}