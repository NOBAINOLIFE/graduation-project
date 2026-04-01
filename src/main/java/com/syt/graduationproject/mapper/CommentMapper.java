package com.syt.graduationproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syt.graduationproject.model.po.CommentPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论表 Mapper 接口
 */
@Mapper
public interface CommentMapper extends BaseMapper<CommentPo> {

}
