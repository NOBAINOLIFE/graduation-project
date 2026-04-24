package com.syt.graduationproject.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class CustomLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        
        if (node.isNumber()) {
            // 处理毫秒时间戳
            long timestamp = node.asLong();
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        } else if (node.isTextual()) {
            String text = node.asText();
            try {
                // 尝试解析标准格式
                return LocalDateTime.parse(text, FORMATTER);
            } catch (Exception e) {
                // 如果失败，尝试ISO格式
                return LocalDateTime.parse(text);
            }
        }
        
        throw new IOException("Unsupported date format: " + node.asText());
    }
}
