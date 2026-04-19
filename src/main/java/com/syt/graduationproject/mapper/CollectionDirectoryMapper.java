package com.syt.graduationproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syt.graduationproject.model.po.CollectionDirectoryPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CollectionDirectoryMapper extends BaseMapper<CollectionDirectoryPo> {

	@Select("SELECT * FROM tb_collection_directory WHERE user_id = #{userId} AND is_default = 1 AND is_deleted = 0 LIMIT 1")
	CollectionDirectoryPo selectDefaultDirectory(@Param("userId") Long userId);
}
