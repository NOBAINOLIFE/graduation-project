package com.syt.graduationproject.model.es;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoEsDoc {

    @Id
    private Long videoId;

    private String title;

    private String description;

    private Long userId;

    private String username;

    private String coverUrl;

    private Long partitionId;

    private String partitionName;

    private List<String> tagList;

    private Long playCount;

    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private Long collectionCount;

    private Integer duration;
}

