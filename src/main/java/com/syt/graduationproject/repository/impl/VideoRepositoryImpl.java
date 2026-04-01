package com.syt.graduationproject.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.syt.graduationproject.enums.VideoStatusEnum;
import com.syt.graduationproject.mapper.VideoMapper;
import com.syt.graduationproject.model.po.VideoPo;
import com.syt.graduationproject.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class VideoRepositoryImpl implements VideoRepository {

    private final VideoMapper videoMapper;

    @Override
    public Long queryUserVideoNum(Long userId) {
        QueryWrapper<VideoPo> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(VideoPo::getUserId, userId)
                .eq(VideoPo::getStatus, VideoStatusEnum.NORMAL.getCode());
        return videoMapper.selectCount(queryWrapper);
    }
}
