package com.syt.graduationproject.consumer;

import com.syt.graduationproject.config.KafkaConfig;
import com.syt.graduationproject.model.kafka.VideoProcessMessage;
import com.syt.graduationproject.service.TranscodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoProcessConsumer {

    private final TranscodeService transcodeService;

    @Resource(name = "transcodeExecutor")
    private ThreadPoolTaskExecutor transcodeExecutor;

    @KafkaListener(topics = KafkaConfig.VIDEO_PROCESS_TOPIC, groupId = "graduation-project-video-process")
    public void consume(VideoProcessMessage message) {
        if (message == null || message.getVideoId() == null) {
            log.warn("忽略非法视频转码消息: {}", message);
            return;
        }
        transcodeExecutor.submit(() -> {
            try {
                transcodeService.processVideo(message.getVideoId(), message.getUserId());
            } catch (Exception e) {
                log.error("处理视频转码消息失败，message: {}", message, e);
            }
        });
    }
}

