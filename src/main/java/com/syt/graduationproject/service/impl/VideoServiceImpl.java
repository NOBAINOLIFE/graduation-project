package com.syt.graduationproject.service.impl;

import com.syt.graduationproject.repository.VideoRepository;
import com.syt.graduationproject.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final VideoRepository videoRepository;

    @Override
    public Long queryVideoNum(Long userId) {
        return videoRepository.queryUserVideoNum(userId);
    }
}
