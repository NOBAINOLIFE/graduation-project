package com.syt.graduationproject.model.es;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEsDoc {

    private Long userId;

    private String username;

    private String avatar;

    private Long fansCount;

    private Long videoCount;

    private String bio;
}

