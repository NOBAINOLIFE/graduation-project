package com.syt.graduationproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syt.graduationproject.model.po.LikeRecordPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 点赞记录表 Mapper 接口
 */
@Mapper
public interface LikeRecordMapper extends BaseMapper<LikeRecordPo> {
}