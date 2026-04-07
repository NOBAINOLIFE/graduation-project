package com.syt.graduationproject.converter;

import com.syt.graduationproject.model.bo.VideoSourceBo;
import com.syt.graduationproject.model.po.VideoSourcePo;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {VideoConverter.class})
public interface VideoConverter {

    List<VideoSourceBo> toVideoSourceBo(List<VideoSourcePo> poList);
}
