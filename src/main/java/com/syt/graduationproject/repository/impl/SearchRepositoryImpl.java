package com.syt.graduationproject.repository.impl;

import com.syt.graduationproject.model.es.UserEsDoc;
import java.util.List;
import java.util.stream.Collectors;
import com.syt.graduationproject.model.es.VideoEsDoc;
import com.syt.graduationproject.repository.SearchRepository;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
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

    /**
     * 查询首页视频播放列表
     */
    @Override
    public List<VideoEsDoc> listVideoPlayList(Long lastVideoId, int size) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        if (lastVideoId != null && lastVideoId > 0) {
            boolQuery.filter(QueryBuilders.rangeQuery("videoId").gt(lastVideoId));
        } else {
            boolQuery.must(QueryBuilders.matchAllQuery());
        }
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(boolQuery)
                .withSort(Sort.by(Sort.Order.asc("videoId")))
                .withMaxResults(size)
                .build();
        SearchHits<VideoEsDoc> hits = elasticsearchRestTemplate
                .search(query, VideoEsDoc.class, IndexCoordinates.of(VIDEO_INDEX));
        return hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    @Override
    public void upsertVideoDoc(VideoEsDoc videoEsDoc) {
        elasticsearchRestTemplate.save(videoEsDoc, IndexCoordinates.of(VIDEO_INDEX));
    }

    @Override
    public void deleteVideoDoc(Long videoId) {
        if (videoId == null) {
            return;
        }
        elasticsearchRestTemplate.delete(String.valueOf(videoId), IndexCoordinates.of(VIDEO_INDEX));
    }
}


