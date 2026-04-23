package com.syt.graduationproject.converter;

import com.syt.graduationproject.enums.VideoResolutionEnum;
import com.syt.graduationproject.model.bo.VideoSourceBo;
import com.syt.graduationproject.model.po.VideoSourcePo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring", uses = {VideoConverter.class})
public interface VideoConverter {

    List<VideoSourceBo> toVideoSourceBoList(List<VideoSourcePo> poList);

    @Mapping(source = "resolutionCode", target = "resolution", qualifiedByName = "resolutionToString")
    VideoSourceBo toVideoSourceBo(VideoSourcePo po);

    @Named("resolutionToString")
    default String resolutionToString(Integer resolution) {
        return VideoResolutionEnum.fromCode(resolution).getName();
    }
}
