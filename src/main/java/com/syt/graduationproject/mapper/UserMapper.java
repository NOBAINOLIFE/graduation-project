package com.syt.graduationproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syt.graduationproject.model.po.UserPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户信息表 Mapper 接口
 */
@Mapper
public interface UserMapper extends BaseMapper<UserPo> {
}