package com.syt.graduationproject.service;

import com.syt.graduationproject.model.request.SearchUserRequest;
import com.syt.graduationproject.model.request.SearchVideoRequest;
import com.syt.graduationproject.model.vo.Page.PageVo;
import com.syt.graduationproject.model.vo.SearchUserVo;
import com.syt.graduationproject.model.vo.SearchVideoVo;

import java.util.List;
import java.util.Map;

public interface SearchService {

    PageVo<SearchVideoVo> searchVideos(SearchVideoRequest request);

    PageVo<SearchUserVo> searchUsers(SearchUserRequest request);

    Map<Long, SearchUserVo> queryUserVoMapFromEs(List<Long> userIds);

    Map<Long, SearchVideoVo> queryVideoVoMapFromEs(List<Long> videoIds);
}

