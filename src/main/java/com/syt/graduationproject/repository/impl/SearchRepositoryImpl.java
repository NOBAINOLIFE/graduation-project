package com.syt.graduationproject.repository.impl;

import com.syt.graduationproject.model.es.UserEsDoc;
import com.syt.graduationproject.model.es.VideoEsDoc;
import com.syt.graduationproject.repository.SearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SearchRepositoryImpl implements SearchRepository {

    public static final String VIDEO_INDEX = "video";

    public static final String USER_INDEX = "user";

    private final ElasticsearchRestTemplate elasticsearchRestTemplate;

    /**
     * 通用搜索方法
     * @param query 查询条件
     * @param clazz 实体类字节码
     * @param indexName 索引名称
     * @param <T> 泛型类型
     */
    @Override
    public <T> SearchHits<T> commonSearch(Query query, Class<T> clazz, String indexName) {
        return elasticsearchRestTemplate.search(
                query,
                clazz,
                IndexCoordinates.of(indexName)
        );
    }

    @Override
    public void upsertVideoDoc(VideoEsDoc videoEsDoc) {
        elasticsearchRestTemplate.save(videoEsDoc, IndexCoordinates.of(VIDEO_INDEX));
    }
}


