package com.syt.graduationproject.config;

import com.syt.graduationproject.repository.impl.SearchRepositoryImpl;
import com.syt.graduationproject.service.EsSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.Settings;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EsIndexInitializer implements ApplicationRunner {

    private final ElasticsearchRestTemplate elasticsearchRestTemplate;

    private final EsSyncService esSyncService;

    @Value("classpath:es-mapping/user-mapping.json")
    private Resource userMappingResource;

    @Value("classpath:es-mapping/video-mapping.json")
    private Resource videoMappingResource;

    @Value("${search.es.rebuild-on-startup:false}")
    private boolean rebuildOnStartup;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        boolean userIndexReady = ensureIndex(
                SearchRepositoryImpl.USER_INDEX,
                userMappingResource,
                rebuildOnStartup
        );
        boolean videoIndexReady = ensureIndex(
                SearchRepositoryImpl.VIDEO_INDEX,
                videoMappingResource,
                rebuildOnStartup
        );

        if (userIndexReady) {
            log.info("开始全量回灌用户索引");
            esSyncService.syncAllUsers();
            log.info("用户索引回灌完成");
        }

        if (videoIndexReady) {
            log.info("开始全量回灌视频索引");
            esSyncService.syncAllPublishedVideos();
            log.info("视频索引回灌完成");
        }
    }

    private boolean ensureIndex(String indexName, Resource mappingResource, boolean rebuild) throws IOException {
        IndexOperations indexOperations = elasticsearchRestTemplate.indexOps(IndexCoordinates.of(indexName));
        boolean exists = indexOperations.exists();
        if (exists && !rebuild) {
            return false;
        }

        if (exists) {
            log.info("开始删除 ES 索引：{}", indexName);
            indexOperations.delete();
        }

        String json = readResource(mappingResource);
        Document rootDocument = Document.parse(json);
        @SuppressWarnings("unchecked")
        Map<String, Object> settingsMap = (Map<String, Object>) rootDocument.get("settings");
        @SuppressWarnings("unchecked")
        Map<String, Object> mappingsMap = (Map<String, Object>) rootDocument.get("mappings");

        Settings settings = settingsMap == null ? new Settings() : new Settings(settingsMap);
        Document mappings = mappingsMap == null ? Document.create() : Document.from(mappingsMap);

        log.info("开始创建 ES 索引：{}", indexName);
        boolean created = indexOperations.create(settings, mappings);
        if (!created) {
            throw new IllegalStateException("创建 ES 索引失败: " + indexName);
        }
        indexOperations.refresh();
        log.info("ES 索引创建完成：{}", indexName);
        return true;
    }

    private String readResource(Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        }
    }
}
