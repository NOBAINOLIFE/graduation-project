package com.syt.graduationproject.converter;

import com.syt.graduationproject.model.po.UserCoinChangeLogPo;
import com.syt.graduationproject.model.vo.UserCoinChangeLogVo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Objects;

import static com.syt.graduationproject.enums.CoinChangeTypeEnum.DAILY_REWARD;
import static com.syt.graduationproject.enums.CoinChangeTypeEnum.VIDEO_REWARD;

@Mapper(componentModel = "spring")
public interface UserConverter {

    List<UserCoinChangeLogVo> toUserCoinChangeLogVoList(List<UserCoinChangeLogPo> poList);

    @Mapping(
            target = "changeDesc",
            expression = "java(buildCoinChangeDesc(po.getChangeType(), po.getVideoId()))"
    )
    @Mapping(target = "relatedTargetId", source = "videoId")
    UserCoinChangeLogVo toUserCoinChangeLogVo(UserCoinChangeLogPo po);

    @Named("buildCoinChangeDesc")
    default String buildCoinChangeDesc(Integer changeType, Long relatedTargetId) {
        if (Objects.equals(changeType, DAILY_REWARD.getCode())) {
            return "每日登录奖励";
        }
        if (Objects.equals(changeType, VIDEO_REWARD.getCode())) {
            return "给视频（id：" + relatedTargetId + "）打赏";
        }
        return "硬币变动";
    }
}
