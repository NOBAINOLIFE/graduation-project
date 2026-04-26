package com.syt.graduationproject.repository;

import java.util.List;

import com.syt.graduationproject.model.es.UserEsDoc;
import com.syt.graduationproject.model.es.VideoEsDoc;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

public interface SearchRepository {

    /**
     * 通用搜索方法
     * @param query 查询条件
     * @param clazz 实体类字节码
     * @param indexName 索引名称
     * @param <T> 泛型类型
     */
    <T> SearchHits<T> commonSearch(Query query, Class<T> clazz, String indexName);

    /**
     * 写入或更新视频索引文档
     */
    void upsertVideoDoc(VideoEsDoc videoEsDoc);

    /**
     * 写入或更新用户索引文档
     */
    void upsertUserDoc(UserEsDoc userEsDoc);

    /**
     * 删除视频索引文档
     */
    void deleteVideoDoc(Long videoId);

    /**
     * 删除用户索引文档
     */
    void deleteUserDoc(Long userId);

    /**
     * 查询首页视频播放列表（去重、分页）
     * @param lastVideoId 上次查询到的最大videoId
     * @param size 查询数量
     * @return 视频ES文档列表
     */
    List<VideoEsDoc> listVideoPlayList(Long lastVideoId, int size);
}

