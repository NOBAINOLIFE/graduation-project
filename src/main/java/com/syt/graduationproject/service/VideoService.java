package com.syt.graduationproject.service;

import com.syt.graduationproject.model.bo.UserVideoInteractionBo;
import com.syt.graduationproject.model.po.UserVideoHistoryPo;
import com.syt.graduationproject.model.vo.VideoPlayDetailVo;

public interface VideoService {

    /**
     * 查询用户视频数
     */
    Long queryVideoNum(Long userId);

    /**
     * 查询播放页视频详情
     */
    VideoPlayDetailVo queryVideoInfo(Long videoId);

}
