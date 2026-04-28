package com.syt.graduationproject.model.vo.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReportInfoVo {

    /**
     * 被举报用户 id
     */
    private Long userId;

    /**
     * 被举报用户昵称
     */
    private String username;

    /**
     * 被举报用户头像地址
     */
    private String avatarUrl;

    /**
     * 被举报用户简介
     */
    private String bio;

    /**
     * 被举报用户状态
     */
    private Integer status;
}
