package com.syt.graduationproject.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syt.graduationproject.model.po.*;

import java.time.LocalDateTime;
import java.util.List;

public interface InteractRepository {

    /**
     * 查询用户粉丝数量
     */
    Long queryUserFansNum(Long myId);

    /**
     * 查询用户关注数量
     */
    Long queryUserFollowNum(Long myId);

    /**
     * 查询用户获赞数量
     */
    Long queryUserLikeNum(Long myId);

    /**
     * 更新用户粉丝数
     */
    void updateUserFansNum(Long userId, Long addNum);

    /**
     * 更新用户关注数
     */
    void updateUserFollowNum(Long userId, Long addNum);

    /**
     * 更新用户获赞数
     */
    void updateUserLikeNum(Long userId, Long addNum);

    /**
     * 更新用户视频数
     */
    void updateUserVideoNum(Long userId, Long addNum);

    /**
     * 初始化用户数据统计信息
     */
    void initUserStats(Long userId);

    /**
     * 查询用户收藏记录（不管是否删除）
     */
    List<CollectionItemPo> queryUserCollectRecord(Long userId, Long videoId, Long directoryId);

    /**
     * 查询用户所有收藏夹
     */
    List<CollectionDirectoryPo> queryUserCollectionDirectory(Long userId);

    /**
     * 查询用户所有收藏有某视频的收藏记录
     */
    List<CollectionItemPo> queryUserCollectionItemWithVideo(Long userId, Long videoId);

    List<CollectionDirectoryPo> batchQueryUserCollectionDirectory(Long userId, List<Long> directoryIdList);

    int batchCollectVideo(Long userId, Long directoryId, List<Long> videoIdList);

    int batchCancelCollectVideo(Long userId, Long directoryId, List<Long> videoIdList);

    /**
     * 查询举报记录
     */
    Page<ReportPo> queryReportRecord(Long reporterId, Integer status, Integer reportType, Integer pageNum, Integer pageSize);

    /**
     * 按热度游标分页查询根评论统计数据
     */
    List<CommentStatsPo> queryRootCommentStatsByHot(Long videoId, Long lastHotScore, LocalDateTime lastCreateTime,
                                                    Long lastCommentId, Integer pageSize);

    /**
     * 统计总根评论数
     */
    Long queryTotalRootCommentNum(Long videoId);
}
