package com.syt.graduationproject.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.syt.graduationproject.mapper.FollowRecordMapper;
import com.syt.graduationproject.model.bo.FollowBo;
import com.syt.graduationproject.model.po.FollowRecordPo;
import com.syt.graduationproject.model.request.FollowRequest;
import com.syt.graduationproject.repository.InteractRepository;
import com.syt.graduationproject.service.InteractService;
import com.syt.graduationproject.util.UserHolderUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

import static com.syt.graduationproject.constant.CommonConstant.NOT_DELETED;

@Service
@RequiredArgsConstructor
public class InteractServiceImpl implements InteractService {

    private final InteractRepository interactRepository;

    private final FollowRecordMapper followRecordMapper;

    /**
     * 查询两者关注关系
     */
    @Override
    public FollowBo queryFollow(Long followerId, Long followeeId) {
        FollowBo followBo = new FollowBo();

        // 1. 查询当前用户(followerId)是否关注了目标用户(followeeId)
        FollowRecordPo followRecordPo1 = interactRepository.queryFollow(followerId, followeeId);
        followBo.setIsFollow(followRecordPo1.getIsDeleted().equals(NOT_DELETED));

        // 2. 查询目标用户(followeeId)是否关注了当前用户(followerId)
        FollowRecordPo followRecordPo2 = interactRepository.queryFollow(followeeId, followerId);
        followBo.setIsFans(followRecordPo2.getIsDeleted().equals(NOT_DELETED));

        return followBo;
    }

    /**
     * 关注/取关
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void follow(FollowRequest request) {
        Long myId = UserHolderUtil.getUser().getUserId();
        Long followeeId = request.getFolloweeId();
        Integer operation = request.getOperation(); // 1: 关注, 0: 取关

        // 查询记录
        LambdaQueryWrapper<FollowRecordPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FollowRecordPo::getFollowerId, myId)
                .eq(FollowRecordPo::getFolloweeId, followeeId);
        FollowRecordPo oldRecord = followRecordMapper.selectOne(wrapper);

        if (operation == 1) {
            // --- 关注逻辑 ---
            if (Objects.isNull(oldRecord)) {
                // 数据库没记录：直接插入
                FollowRecordPo record = new FollowRecordPo();
                record.setFollowerId(myId);
                record.setFolloweeId(followeeId);
                followRecordMapper.insert(record);
            } else if (oldRecord.getIsDeleted() == 1) {
                // 数据库有记录但已取关：手动把 1 改回 0
                oldRecord.setIsDeleted(0);
                followRecordMapper.updateById(oldRecord);
            }
            // 更新关注数和粉丝数
            interactRepository.updateUserFollowNum(myId, 1L);
            interactRepository.updateUserFansNum(followeeId, 1L);
        } else {
            // --- 取关逻辑 ---
            // 只有当前是关注状态，才执行逻辑删除
            if (Objects.nonNull(oldRecord) && oldRecord.getIsDeleted() == 0) {
                oldRecord.setIsDeleted(1);
                followRecordMapper.updateById(oldRecord);
                // 更新关注数和粉丝数
                interactRepository.updateUserFollowNum(myId, -1L);
                interactRepository.updateUserFansNum(followeeId, -1L);
            }
        }
    }
}
