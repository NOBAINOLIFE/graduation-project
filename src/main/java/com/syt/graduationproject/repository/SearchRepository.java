package com.syt.graduationproject.repository;

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
}

