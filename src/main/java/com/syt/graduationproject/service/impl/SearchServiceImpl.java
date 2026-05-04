package com.syt.graduationproject.service.impl;

import com.syt.graduationproject.model.es.UserEsDoc;
import com.syt.graduationproject.model.es.VideoEsDoc;
import com.syt.graduationproject.model.request.SearchUserRequest;
import com.syt.graduationproject.model.request.SearchVideoRequest;
import com.syt.graduationproject.model.vo.Page.PageVo;
import com.syt.graduationproject.model.vo.SearchUserVo;
import com.syt.graduationproject.model.vo.SearchVideoVo;
import com.syt.graduationproject.repository.SearchRepository;
import com.syt.graduationproject.service.InteractRelationService;
import com.syt.graduationproject.service.SearchService;
import com.syt.graduationproject.converter.SearchConverter;
import com.syt.graduationproject.util.UserHolderUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.syt.graduationproject.repository.impl.SearchRepositoryImpl.USER_INDEX;
import static com.syt.graduationproject.repository.impl.SearchRepositoryImpl.VIDEO_INDEX;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private static final int VIDEO_SORT_PLAY_COUNT = 1;

    private static final int VIDEO_SORT_LATEST = 2;

    private static final int VIDEO_SORT_COLLECTION_COUNT = 3;

    private static final int USER_SORT_FANS_COUNT = 1;

    private static final int DEFAULT_PAGE_NUM = 1;

    private static final int DEFAULT_PAGE_SIZE = 10;

    private static final int MAX_PAGE_SIZE = 100;

    private final SearchRepository searchRepository;

    private final SearchConverter searchConverter;

    private final InteractRelationService interactRelationService;

    @Override
    public PageVo<SearchVideoVo> searchVideos(SearchVideoRequest request) {
        NativeSearchQuery query = buildVideoQuery(request);
        SearchHits<VideoEsDoc> searchHits = searchRepository.commonSearch(query, VideoEsDoc.class, VIDEO_INDEX);
        List<SearchVideoVo> records = searchHits.getSearchHits()
                .stream()
                .map(SearchHit::getContent)
                .map(searchConverter::toSearchVideoVo)
                .collect(Collectors.toList());

        return PageVo.<SearchVideoVo>builder()
                .total(searchHits.getTotalHits())
                .pageNum(normalizePageNum(request.getPageNum()))
                .pageSize(normalizePageSize(request.getPageSize()))
                .records(records)
                .build();
    }

    @Override
    public PageVo<SearchUserVo> searchUsers(SearchUserRequest request) {
        NativeSearchQuery query = buildUserQuery(request);
        SearchHits<UserEsDoc> searchHits = searchRepository.commonSearch(query, UserEsDoc.class, USER_INDEX);

        Long userId = UserHolderUtil.getUser().getUserId();
        List<SearchUserVo> records = searchHits.getSearchHits()
                .stream()
                .map(SearchHit::getContent)
                .map(searchConverter::toSearchUserVo)
                .peek(userVo -> {
                    if (userId != null) {
                        userVo.setIsFollow(interactRelationService.queryFollowRelation(userId, userVo.getUserId()).getIsFollow());
                    } else {
                        userVo.setIsFollow(false);
                    }
                })
                .collect(Collectors.toList());

        return PageVo.<SearchUserVo>builder()
                .total(searchHits.getTotalHits())
                .pageNum(normalizePageNum(request.getPageNum()))
                .pageSize(normalizePageSize(request.getPageSize()))
                .records(records)
                .build();
    }

    @Override
    public Map<Long, SearchUserVo> queryUserVoMapFromEs(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.boolQuery()
                        .filter(QueryBuilders.termsQuery("userId", userIds)))
                .withMaxResults(userIds.size())
                .build();
        SearchHits<UserEsDoc> searchHits = searchRepository.commonSearch(query, UserEsDoc.class, USER_INDEX);
        return searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(searchConverter::toSearchUserVo)
                .collect(Collectors.toMap(SearchUserVo::getUserId, item -> item, (left, right) -> left));
    }

    @Override
    public Map<Long, SearchVideoVo> queryVideoVoMapFromEs(List<Long> videoIds) {
        if (CollectionUtils.isEmpty(videoIds)) {
            return Collections.emptyMap();
        }
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.boolQuery()
                        .filter(QueryBuilders.termsQuery("videoId", videoIds)))
                .withMaxResults(videoIds.size())
                .build();
        SearchHits<VideoEsDoc> searchHits = searchRepository.commonSearch(query, VideoEsDoc.class, VIDEO_INDEX);
        return searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(searchConverter::toSearchVideoVo)
                .collect(Collectors.toMap(SearchVideoVo::getVideoId, item -> item, (left, right) -> left));
    }

    /**
     * 根据关键词、时长、发布时间和排序方式构建视频检索条件
     */
    private NativeSearchQuery buildVideoQuery(SearchVideoRequest request) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if (request.getUserId() != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("userId", request.getUserId()));
        }
        if (request.getPartitionId() != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("partitionId", request.getPartitionId()));
        }
        
        if (StringUtils.isNotBlank(request.getKeyword())) {
            BoolQueryBuilder keywordQuery = QueryBuilders.boolQuery()
                    .should(QueryBuilders.matchQuery("title", request.getKeyword()))
                    .should(QueryBuilders.matchQuery("title.ngram", request.getKeyword()))
                    .minimumShouldMatch(1);
            boolQueryBuilder.must(keywordQuery);
        } else {
            boolQueryBuilder.must(QueryBuilders.matchAllQuery());
        }

        if (request.getMinDuration() != null || request.getMaxDuration() != null) {
            if (request.getMinDuration() != null) {
                boolQueryBuilder.filter(QueryBuilders.rangeQuery("duration").gte(request.getMinDuration()));
            }
            if (request.getMaxDuration() != null) {
                boolQueryBuilder.filter(QueryBuilders.rangeQuery("duration").lte(request.getMaxDuration()));
            }
        }

        if (request.getPublishStartTime() != null || request.getPublishEndTime() != null) {
            if (request.getPublishStartTime() != null) {
                boolQueryBuilder.filter(QueryBuilders.rangeQuery("createTime").gte(request.getPublishStartTime()));
            }
            if (request.getPublishEndTime() != null) {
                boolQueryBuilder.filter(QueryBuilders.rangeQuery("createTime").lte(request.getPublishEndTime()));
            }
        }

        Sort sort;
        Integer sortType = request.getSortType();
        if (sortType == null) {
            sort = Sort.by(Sort.Order.desc("_score"), Sort.Order.desc("createTime"));
        } else {
            switch (sortType) {
                case VIDEO_SORT_PLAY_COUNT:
                    sort = Sort.by(Sort.Order.desc("playCount"), Sort.Order.desc("createTime"));
                    break;
                case VIDEO_SORT_LATEST:
                    sort = Sort.by(Sort.Order.desc("createTime"));
                    break;
                case VIDEO_SORT_COLLECTION_COUNT:
                    sort = Sort.by(Sort.Order.desc("collectionCount"), Sort.Order.desc("createTime"));
                    break;
                default:
                    sort = Sort.by(Sort.Order.desc("_score"), Sort.Order.desc("createTime"));
                    break;
            }
        }

        Pageable pageable = PageRequest.of(
                normalizePageNum(request.getPageNum()) - 1,
                normalizePageSize(request.getPageSize()),
                sort
        );

        return new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .withPageable(pageable)
                .build();
    }

    private NativeSearchQuery buildUserQuery(SearchUserRequest request) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if (StringUtils.isNotBlank(request.getKeyword())) {
            BoolQueryBuilder keywordQuery = QueryBuilders.boolQuery()
                    .should(QueryBuilders.matchQuery("username", request.getKeyword()))
                    .should(QueryBuilders.matchQuery("username.ngram", request.getKeyword()))
                    .minimumShouldMatch(1);
            boolQueryBuilder.must(keywordQuery);
        } else {
            boolQueryBuilder.must(QueryBuilders.matchAllQuery());
        }


        Sort sort;
        if (request.getSortType() != null && request.getSortType() == USER_SORT_FANS_COUNT) {
            sort = Sort.by(Sort.Order.desc("fansCount"), Sort.Order.desc("_score"));
        } else {
            sort = Sort.by(Sort.Order.desc("_score"));
        }

        Pageable pageable = PageRequest.of(
                normalizePageNum(request.getPageNum()) - 1,
                normalizePageSize(request.getPageSize()),
                sort
        );

        return new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .withPageable(pageable)
                .build();
    }

    private Integer normalizePageNum(Integer pageNum) {
        if (pageNum == null || pageNum < 1) {
            return DEFAULT_PAGE_NUM;
        }
        return pageNum;
    }

    private Integer normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}

