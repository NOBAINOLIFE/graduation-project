package com.syt.graduationproject.repository;

public interface VideoRepository {

    /**
     * 查询用户视频数
     */
    Long queryUserVideoNum(Long userId);
}
