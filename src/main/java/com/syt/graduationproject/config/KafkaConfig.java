package com.syt.graduationproject.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String VIDEO_PROCESS_TOPIC = "video-process-topic";

    @Bean
    public NewTopic videoProcessTopic() {
        return TopicBuilder.name(VIDEO_PROCESS_TOPIC)
                .partitions(3)
                .build();
    }
}
