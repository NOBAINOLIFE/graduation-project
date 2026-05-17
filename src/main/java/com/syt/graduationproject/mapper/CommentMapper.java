package com.syt.graduationproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syt.graduationproject.model.po.CommentPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 评论表 Mapper 接口
 */
@Mapper
public interface CommentMapper extends BaseMapper<CommentPo> {

	@Update("UPDATE tb_comment SET is_deleted = 1 WHERE id = #{commentId} AND is_deleted = 0")
	int logicDeleteById(@Param("commentId") Long commentId);

	@Update("UPDATE tb_comment SET is_deleted = 1 WHERE ((id = #{rootCommentId}) OR (root_id = #{rootCommentId})) AND is_deleted = 0")
	int logicDeleteRootTree(@Param("rootCommentId") Long rootCommentId);

	@Update("UPDATE tb_comment SET is_top = 0 WHERE video_id = #{videoId} AND root_id = 0 AND is_deleted = 0")
	int clearTopByVideoId(@Param("videoId") Long videoId);

	@Update("UPDATE tb_comment SET is_top = #{isTop} WHERE id = #{commentId} AND root_id = 0 AND is_deleted = 0")
	int updateTopById(@Param("commentId") Long commentId, @Param("isTop") Integer isTop);

	List<CommentPo> queryCreatorComments(@Param("userId") Long userId,
										 @Param("keyword") String keyword,
										 @Param("videoId") Long videoId,
										 @Param("sortType") Integer sortType,
										 @Param("offset") Long offset,
										 @Param("pageSize") Integer pageSize);

	Long countCreatorComments(@Param("userId") Long userId,
							  @Param("keyword") String keyword,
							  @Param("videoId") Long videoId);

}
