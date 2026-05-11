package com.syt.graduationproject.service.impl;

import com.syt.graduationproject.enums.UserStatusEnum;
import com.syt.graduationproject.enums.RoleEnum;
import com.syt.graduationproject.exception.ErrorOperationException;
import com.syt.graduationproject.exception.ErrorParamException;
import com.syt.graduationproject.mapper.CollectionDirectoryMapper;
import com.syt.graduationproject.mapper.UserCoinChangeLogMapper;
import com.syt.graduationproject.mapper.UserRoleMapper;
import com.syt.graduationproject.mapper.UserMapper;
import com.syt.graduationproject.model.bo.FollowBo;
import com.syt.graduationproject.model.po.CollectionDirectoryPo;
import com.syt.graduationproject.model.po.UserCoinChangeLogPo;
import com.syt.graduationproject.model.po.UserPo;
import com.syt.graduationproject.model.po.UserRoleRelPo;
import com.syt.graduationproject.model.po.UserStatsPo;
import com.syt.graduationproject.model.request.LoginRequest;
import com.syt.graduationproject.model.request.RegisterRequest;
import com.syt.graduationproject.model.request.UserInfoUpdateRequest;
import com.syt.graduationproject.model.request.UserPasswordUpdateRequest;
import com.syt.graduationproject.model.vo.LoginVo;
import com.syt.graduationproject.model.vo.UserCoinChangeLogVo;
import com.syt.graduationproject.model.vo.UserInfoVo;
import com.syt.graduationproject.repository.InteractRepository;
import com.syt.graduationproject.repository.UserRepository;
import com.syt.graduationproject.repository.VideoRepository;
import com.syt.graduationproject.service.EsSyncService;
import com.syt.graduationproject.service.InteractRelationService;
import com.syt.graduationproject.service.InteractService;
import com.syt.graduationproject.service.minio.MinioService;
import com.syt.graduationproject.service.UserService;
import com.syt.graduationproject.util.JsonUtil;
import com.syt.graduationproject.util.JwtUtil;
import com.syt.graduationproject.util.PasswordUtil;
import com.syt.graduationproject.util.RedisKeyUtil;
import com.syt.graduationproject.util.UserHolderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.syt.graduationproject.constant.UserConstant.*;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int COIN_CHANGE_TYPE_DAILY_REWARD = 1;

    private static final int COIN_CHANGE_TYPE_VIDEO_REWARD = 2;

    private static final long USER_INFO_CACHE_TTL_MINUTES = 30L;

    private final InteractService interactService;

    private final UserRepository userRepository;

    private final VideoRepository videoRepository;

    private final InteractRepository interactRepository;

    private final EsSyncService esSyncService;

    private final UserMapper userMapper;

    private final UserRoleMapper userRoleMapper;

    private final UserCoinChangeLogMapper userCoinChangeLogMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final MinioService minioService;

    private final CollectionDirectoryMapper collectionDirectoryMapper;

    private final InteractRelationService interactRelationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request) {
        Long account = request.getAccount();
        String password = request.getPassword();
        String username = request.getUsername();

        PasswordUtil.validate(password);

        // 判断用户是否存在
        UserPo userPo = userRepository.queryUserByAccount(account);
        if (Objects.nonNull(userPo)) {
            log.warn("用户重复注册，用户账号：{}", account);
            throw new ErrorOperationException("该账号已经注册");
        }

        // 添加用户
        UserPo newUser = UserPo.builder()
                .account(account)
                .password(PasswordUtil.md5(password))
                .username(username)
                .avatarUrl(DEFAULT_AVATAR)
                .bio(DEFAULT_BIO)
                .build();
        userMapper.insert(newUser);
        stringRedisTemplate.delete(RedisKeyUtil.userInfoKey(newUser.getId()));

        UserRoleRelPo userRoleRelPo = UserRoleRelPo.builder()
                .userId(newUser.getId())
                .roleId(RoleEnum.USER.getRoleId())
                .build();
        userRoleMapper.insert(userRoleRelPo);

        // 初始化用户数据统计信息
        interactRepository.initUserStats(newUser.getId());

        // 创建默认收藏夹
        CollectionDirectoryPo collectionDirectory = CollectionDirectoryPo.builder()
                .userId(newUser.getId())
                .name("默认收藏夹")
                .coverUrl(DEFAULT_COLLECTION_DIRECTORY_COVER)
                .isPublic(1)
                .isDefault(1)
                .isDeleted(0)
                .build();
        collectionDirectoryMapper.insert(collectionDirectory);
        esSyncService.syncUser(newUser.getId());
        log.info("用户注册成功，注册请求：{}", request);
    }

    /**
     * 用户登录
     */
    @Override
    public LoginVo login(LoginRequest request) {
        Long account = request.getAccount();
        String password = request.getPassword();

        // 判断用户是否存在
        UserPo userPo = userRepository.queryUserByAccount(account);
        if (Objects.isNull(userPo)) {
            log.warn("未注册用户尝试登录，用户账号：{}", account);
            throw new ErrorOperationException("用户不存在");
        }

        if (userPo.getStatus().equals(UserStatusEnum.BANNED.getCode())) {
            throw new ErrorOperationException("该账号已被禁用");
        }

        if (userPo.getStatus().equals(UserStatusEnum.DELETED.getCode())) {
            throw new ErrorOperationException("该账号已被注销");
        }

        // 校验密码
        if (!userPo.getPassword().equals(PasswordUtil.md5(password))) {
            throw new ErrorParamException("账号或密码错误");
        }

        // 生成 JwtToken
        Map<String, Object> claimMap = new HashMap<>();
        Long userId = userPo.getId();
        Long roleId = RoleEnum.USER.getRoleId();
        UserRoleRelPo userRoleRelPo = userRoleMapper.selectOne(new QueryWrapper<UserRoleRelPo>()
                .lambda()
                .eq(UserRoleRelPo::getUserId, userId));
        if (userRoleRelPo != null && userRoleRelPo.getRoleId() != null) {
            roleId = userRoleRelPo.getRoleId();
        }
        RoleEnum roleEnum = RoleEnum.fromRoleId(roleId);
        String roleCode = roleEnum == null ? RoleEnum.USER.getRoleCode() : roleEnum.getRoleCode();

        // 管理员和用户登录入口分离：管理员只能从后台登录，用户只能从前台登录
        boolean isAdmin = RoleEnum.ADMIN.getRoleCode().equalsIgnoreCase(roleCode);
        boolean isAdminLogin = Boolean.TRUE.equals(request.getIsAdminLogin());
        if (isAdmin && !isAdminLogin) {
            throw new ErrorOperationException("管理员请使用管理后台登录");
        }
        if (!isAdmin && isAdminLogin) {
            throw new ErrorOperationException("该账号不是管理员");
        }

        claimMap.put(USER_ID, userId);
        claimMap.put(USERNAME, userPo.getUsername());
        claimMap.put(ROLE_ID, roleId);
        claimMap.put(ROLE_CODE, roleCode);
        String jwtToken = JwtUtil.generateJwtToken(claimMap);
        log.info("用户登录成功，用户ID：{}，用户名：{}，JWT令牌：{}", userId, userPo.getUsername(), jwtToken);

        // 加入 Redis 白名单
        stringRedisTemplate.opsForValue()
                .set(RedisKeyUtil.jwtWhitelistKey(userId), jwtToken, JwtUtil.EXPIRATION, TimeUnit.MILLISECONDS);
        stringRedisTemplate.delete(RedisKeyUtil.jwtPreviousWhitelistKey(userId));
        interactService.claimDailyCoin(userId);
        return LoginVo.builder()
                .userId(userId)
                .username(userPo.getUsername())
                .avatarUrl(userPo.getAvatarUrl())
                .roleCode(roleCode)
                .token(jwtToken)
                .build();
    }

    /**
     * 查询用户信息
     */
    @Override
    public UserInfoVo queryUserInfo(Long userId) {
        Long myId = UserHolderUtil.getUser().getUserId();

        // 查询用户是否存在
        UserPo userPo = queryUserByIdWithCache(userId);
        if (Objects.isNull(userPo)) {
            log.warn("用户不存在，用户ID：{}", userId);
            throw new ErrorOperationException("用户不存在");
        }

        // 判断用户状态
        Integer status = userPo.getStatus();
        if (status.equals(UserStatusEnum.DELETED.getCode())) {
            throw new ErrorOperationException("该用户已注销");
        }
        UserInfoVo userInfoVo = UserInfoVo.builder()
                .userId(userPo.getId())
                .username(userPo.getUsername())
                .avatarUrl(userPo.getAvatarUrl())
                .bio(userPo.getBio())
                .isBanned(userPo.getStatus().equals(UserStatusEnum.BANNED.getCode()))
                .build();

        // 查询关注信息
        FollowBo followBo = interactRelationService.queryFollowRelation(myId, userId);
        userInfoVo.setIsFollow(followBo.getIsFollow());
        userInfoVo.setIsFans(followBo.getIsFans());

        // 查询各种数量
        UserStatsPo userStatsPo = userRepository.queryUserStatsById(userId);
        userInfoVo.setVideoNum(userStatsPo == null ? 0L : userStatsPo.getVideoNum());
        userInfoVo.setFansNum(userStatsPo == null ? 0L : userStatsPo.getFansNum());
        userInfoVo.setFollowNum(userStatsPo == null ? 0L : userStatsPo.getFollowNum());
        userInfoVo.setLikeNum(userStatsPo == null ? 0L : userStatsPo.getLikeNum());
        userInfoVo.setPlayNum(userStatsPo == null ? 0L : userStatsPo.getPlayNum());

        // 设置黑名单
        userInfoVo.setIsBlack(interactService.hasMutualBlock(myId, userId));

        return userInfoVo;
    }

    @Override
    public List<UserCoinChangeLogVo> queryMyCoinChangeLogs(Integer days) {
        Long userId = UserHolderUtil.getUser().getUserId();
        int queryDays = days == null ? 7 : days;
        if (queryDays <= 0 || queryDays > 30) {
            throw new ErrorParamException("查询天数需在1到30之间");
        }
        LocalDateTime startTime = LocalDate.now().minusDays(queryDays - 1L).atStartOfDay();

        List<UserCoinChangeLogVo> result = userCoinChangeLogMapper.selectList(new QueryWrapper<UserCoinChangeLogPo>()
                        .lambda()
                        .eq(UserCoinChangeLogPo::getUserId, userId)
                        .ge(UserCoinChangeLogPo::getCreateTime, startTime)
                        .orderByDesc(UserCoinChangeLogPo::getCreateTime, UserCoinChangeLogPo::getId))
                .stream()
                .map(this::toCoinChangeLogVo)
                .collect(Collectors.toList());
        result.sort((left, right) -> {
            LocalDateTime leftTime = left.getCreateTime();
            LocalDateTime rightTime = right.getCreateTime();
            if (leftTime == null && rightTime == null) {
                return Long.compare(
                        right.getId() == null ? 0L : right.getId(),
                        left.getId() == null ? 0L : left.getId()
                );
            }
            if (leftTime == null) {
                return 1;
            }
            if (rightTime == null) {
                return -1;
            }
            int timeCompare = rightTime.compareTo(leftTime);
            if (timeCompare != 0) {
                return timeCompare;
            }
            return Long.compare(
                    right.getId() == null ? 0L : right.getId(),
                    left.getId() == null ? 0L : left.getId()
            );
        });
        return result;
    }

    private UserCoinChangeLogVo toCoinChangeLogVo(UserCoinChangeLogPo item) {
        return UserCoinChangeLogVo.builder()
                .id(item.getId())
                .changeAmount(item.getChangeAmount())
                .changeType(item.getChangeType())
                .relatedTargetId(item.getRelatedTargetId())
                .changeDesc(buildCoinChangeDesc(item.getChangeType(), item.getRelatedTargetId()))
                .createTime(item.getCreateTime())
                .build();
    }

    private String buildCoinChangeDesc(Integer changeType, Long relatedTargetId) {
        if (Objects.equals(changeType, COIN_CHANGE_TYPE_DAILY_REWARD)) {
            return "每日登录奖励";
        }
        if (Objects.equals(changeType, COIN_CHANGE_TYPE_VIDEO_REWARD)) {
            return "给视频（id：" + relatedTargetId + "）打赏";
        }
        return "硬币变动";
    }

    @Override
    public void updateUserInfo(Long userId, UserInfoUpdateRequest request) {
        UserPo userPo = userRepository.queryUserById(userId);
        if (userPo == null) {
            throw new ErrorOperationException("用户不存在");
        }
        boolean changed = false;
        boolean usernameChanged = false;
        if (request.getUsername() != null) {
            usernameChanged = !Objects.equals(userPo.getUsername(), request.getUsername());
            userPo.setUsername(request.getUsername());
            changed = true;
        }
        if (request.getAvatarUrl() != null && request.getAvatarUrl().startsWith("data:")) {
            throw new ErrorOperationException("请通过头像上传接口上传头像文件");
        } else if (request.getAvatarUrl() != null) {
            userPo.setAvatarUrl(request.getAvatarUrl());
            changed = true;
        }
        if (request.getBio() != null) {
            userPo.setBio(request.getBio());
            changed = true;
        }
        if (changed) {
            userMapper.updateById(userPo);
            stringRedisTemplate.delete(RedisKeyUtil.userInfoKey(userId));
            esSyncService.syncUser(userId);
            if (usernameChanged) {
                esSyncService.syncPublishedVideosByUserId(userId);
            }
        }
    }

    @Override
    public String uploadAvatar(MultipartFile file) {
        try {
            // 头像上传到 avatars 文件夹
            return minioService.uploadFile(file, "avatars/");
        } catch (Exception e) {
            log.error("上传头像到Minio失败", e);
            throw new ErrorOperationException("上传头像失败");
        }
    }

    @Override
    public void logout(Long userId) {
        stringRedisTemplate.delete(RedisKeyUtil.jwtWhitelistKey(userId));
        stringRedisTemplate.delete(RedisKeyUtil.jwtPreviousWhitelistKey(userId));
    }

    @Override
    public void updatePassword(Long userId, UserPasswordUpdateRequest request) {
        UserPo userPo = userRepository.queryUserById(userId);
        if (userPo == null) {
            throw new ErrorOperationException("用户不存在");
        }

        PasswordUtil.validate(request.getNewPassword());

        if (!userPo.getPassword().equals(PasswordUtil.md5(request.getOldPassword()))) {
            throw new ErrorParamException("原密码错误");
        }
        userPo.setPassword(PasswordUtil.md5(request.getNewPassword()));
        userMapper.updateById(userPo);
        stringRedisTemplate.delete(RedisKeyUtil.userInfoKey(userId));
    }

    private UserPo queryUserByIdWithCache(Long userId) {
        String cacheKey = RedisKeyUtil.userInfoKey(userId);
        String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.isNotBlank(cachedJson)) {
            return JsonUtil.fromJson(cachedJson, UserPo.class);
        }
        UserPo userPo = userRepository.queryUserById(userId);
        if (userPo != null) {
            stringRedisTemplate.opsForValue().set(cacheKey, JsonUtil.toJson(userPo),
                    USER_INFO_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        }
        return userPo;
    }
}
