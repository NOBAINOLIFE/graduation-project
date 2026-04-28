package com.syt.graduationproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syt.graduationproject.model.po.PrivateMessagePo;
import com.syt.graduationproject.model.vo.ChatSessionVo;
import com.syt.graduationproject.model.vo.PrivateMessageVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;

@Mapper
public interface PrivateMessageMapper extends BaseMapper<PrivateMessagePo> {

    /**
     * 历史消息（游标分页：beforeId 为空表示取最新）
     * 对应 XML: queryHistory
     */
    List<PrivateMessageVo> queryHistory(@Param("myId") Long myId,
                                       @Param("withUserId") Long withUserId,
                                       @Param("beforeId") Long beforeId,
                                       @Param("pageSize") Integer pageSize);

    /**
     * 会话列表（按双方 userId 聚合出最近一条 + 未读数）
     * 对应 XML: querySessions
     */
    List<ChatSessionVo> querySessions(@Param("myId") Long myId, @Param("readStatus") Integer readStatus);

    /**
     * 总未读数（所有会话累加）
     */
    @Select("select count(1) from tb_private_message where to_user_id = #{myId} and (status <> #{readStatus})")
    Long queryTotalUnread(@Param("myId") Long myId, @Param("readStatus") Integer readStatus);

    /**
     * 标记已读（把对方 -> 我 的消息，id<=upToMsgId 的置为 READ）
     * 对应 XML: markRead
     */
    int markRead(@Param("myId") Long myId,
                 @Param("withUserId") Long withUserId,
                 @Param("upToMsgId") Long upToMsgId,
                 @Param("readStatus") Integer readStatus,
                 @Param("readTime") LocalDateTime readTime);

    /**
     * 按消息ID批量标记已读（用于离线消息上线推送后批量更新）
     * 对应 XML: markReadByIds
     */
    int markReadByIds(@Param("myId") Long myId,
                      @Param("ids") Collection<Long> ids,
                      @Param("readStatus") Integer readStatus,
                      @Param("readTime") LocalDateTime readTime);

    /**
     * 按消息ID批量标记已投递（用于离线消息上线推送成功后批量更新）
     */
    int markDeliveredByIds(@Param("myId") Long myId,
                           @Param("ids") Collection<Long> ids,
                           @Param("deliveredStatus") Integer deliveredStatus,
                           @Param("deliveredTime") LocalDateTime deliveredTime,
                           @Param("savedStatus") Integer savedStatus);

    /**
     * 标记接收确认。
     * 允许在消息已经 READ 的情况下补写 acked_time / delivered_time，而不回退状态。
     */
    int markAcked(@Param("myId") Long myId,
                  @Param("serverMsgId") Long serverMsgId,
                  @Param("ackedStatus") Integer ackedStatus,
                  @Param("ackedTime") LocalDateTime ackedTime,
                  @Param("savedStatus") Integer savedStatus,
                  @Param("deliveredStatus") Integer deliveredStatus,
                  @Param("failStatus") Integer failStatus);
}
