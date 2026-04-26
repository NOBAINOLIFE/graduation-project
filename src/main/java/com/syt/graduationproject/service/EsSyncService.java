package com.syt.graduationproject.service;

public interface EsSyncService {

    /**
     * 按用户 ID 重建并同步用户 ES 文档
     */
    void syncUser(Long userId);

    /**
     * 删除用户 ES 文档
     */
    void deleteUser(Long userId);

    /**
     * 按视频 ID 重建并同步视频 ES 文档
     */
    void syncVideo(Long videoId);

    /**
     * 删除视频 ES 文档
     */
    void deleteVideo(Long videoId);

    /**
     * 重刷某个用户名下所有已发布视频的 ES 文档
     */
    void syncPublishedVideosByUserId(Long userId);

    /**
     * 全量重刷所有正常用户到 ES
     */
    void syncAllUsers();

    /**
     * 全量重刷所有已发布视频到 ES
     */
    void syncAllPublishedVideos();
}
