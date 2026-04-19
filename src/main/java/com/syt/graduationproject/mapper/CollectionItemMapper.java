package com.syt.graduationproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syt.graduationproject.model.po.CollectionItemPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CollectionItemMapper extends BaseMapper<CollectionItemPo> {

	@Select("SELECT COUNT(1) FROM tb_collection_item WHERE directory_id = #{directoryId} AND is_deleted = 0")
	Long countByDirectory(@Param("directoryId") Long directoryId);
}
