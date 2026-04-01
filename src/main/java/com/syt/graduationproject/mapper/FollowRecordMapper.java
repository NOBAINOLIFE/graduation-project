package com.syt.graduationproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syt.graduationproject.model.po.FollowRecordPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 关注记录表 Mapper 接口
 */
@Mapper
public interface FollowRecordMapper extends BaseMapper<FollowRecordPo> {

    Integer FOLLOW = 0;

    Integer UNFOLLOW = 1;
}