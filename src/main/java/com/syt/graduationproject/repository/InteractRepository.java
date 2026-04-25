package com.syt.graduationproject.repository;

import com.syt.graduationproject.model.po.CollectionDirectoryPo;
import com.syt.graduationproject.model.po.CollectionItemPo;
import com.syt.graduationproject.model.po.FollowRecordPo;

import java.util.List;

public interface InteractRepository {

    /**
     * 查询两者关注关系
     */
    FollowRecordPo queryFollow(Long followerId, Long followeeId);

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

    boolean isCollectVideo(Long userId, Long videoId);

    List<CollectionDirectoryPo> batchQueryUserCollectionDirectory(Long userId, List<Long> directoryIdList);

    int batchCollectVideo(Long userId, Long directoryId, List<Long> videoIdList);

    int batchCancelCollectVideo(Long userId, Long directoryId, List<Long> videoIdList);
}
