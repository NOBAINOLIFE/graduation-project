package com.syt.graduationproject.service;

import com.syt.graduationproject.model.request.SearchUserRequest;
import com.syt.graduationproject.model.request.SearchVideoRequest;
import com.syt.graduationproject.model.vo.SearchPageVo;
import com.syt.graduationproject.model.vo.SearchUserVo;
import com.syt.graduationproject.model.vo.SearchVideoVo;

public interface SearchService {

    SearchPageVo<SearchVideoVo> searchVideos(SearchVideoRequest request);

    SearchPageVo<SearchUserVo> searchUsers(SearchUserRequest request);
}

