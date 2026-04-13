package com.syt.graduationproject.converter;

import com.syt.graduationproject.model.es.UserEsDoc;
import com.syt.graduationproject.model.es.VideoEsDoc;
import com.syt.graduationproject.model.vo.SearchUserVo;
import com.syt.graduationproject.model.vo.SearchVideoVo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SearchConverter {

    SearchVideoVo toSearchVideoVo(VideoEsDoc videoEsDoc);

    SearchUserVo toSearchUserVo(UserEsDoc userEsDoc);
}


