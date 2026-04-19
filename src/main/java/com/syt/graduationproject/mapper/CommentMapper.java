package com.syt.graduationproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syt.graduationproject.model.po.CommentPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 评论表 Mapper 接口
 */
@Mapper
public interface CommentMapper extends BaseMapper<CommentPo> {

	@Update("UPDATE tb_comment SET is_deleted = 1 WHERE id = #{commentId} AND is_deleted = 0")
	int logicDeleteById(@Param("commentId") Long commentId);

	@Update("UPDATE tb_comment SET is_deleted = 1 WHERE ((id = #{rootCommentId}) OR (root_id = #{rootCommentId})) AND is_deleted = 0")
	int logicDeleteRootTree(@Param("rootCommentId") Long rootCommentId);

}
