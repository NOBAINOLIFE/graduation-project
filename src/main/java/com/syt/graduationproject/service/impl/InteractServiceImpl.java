package com.syt.graduationproject.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.syt.graduationproject.mapper.FollowRecordMapper;
import com.syt.graduationproject.model.bo.FollowBo;
import com.syt.graduationproject.model.po.FollowRecordPo;
import com.syt.graduationproject.repository.InteractRepository;
import com.syt.graduationproject.service.InteractService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.syt.graduationproject.mapper.FollowRecordMapper.FOLLOW;

@Service
@RequiredArgsConstructor
public class InteractServiceImpl implements InteractService {

    private final InteractRepository interactRepository;

    @Override
    public FollowBo queryFollow(Long followerId, Long followeeId) {
        FollowBo followBo = new FollowBo();

        // 1. 查询当前用户(followerId)是否关注了目标用户(followeeId)
        FollowRecordPo followRecordPo1 = interactRepository.queryFollow(followerId, followeeId);
        followBo.setIsFollow(followRecordPo1.getStatus().equals(FOLLOW));

        // 2. 查询目标用户(followeeId)是否关注了当前用户(followerId)
        FollowRecordPo followRecordPo2 = interactRepository.queryFollow(followeeId, followerId);
        followBo.setIsFans(followRecordPo2.getStatus().equals(FOLLOW));

        return followBo;
    }
}
