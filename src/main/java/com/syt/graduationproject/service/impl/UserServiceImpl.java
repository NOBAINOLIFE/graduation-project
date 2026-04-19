package com.syt.graduationproject.service.impl;

import com.syt.graduationproject.enums.UserStatusEnum;
import com.syt.graduationproject.enums.RoleEnum;
import com.syt.graduationproject.exception.ErrorOperationException;
import com.syt.graduationproject.exception.ErrorParamException;
import com.syt.graduationproject.mapper.UserRoleMapper;
import com.syt.graduationproject.mapper.UserMapper;
import com.syt.graduationproject.model.bo.FollowBo;
import com.syt.graduationproject.model.po.UserPo;
import com.syt.graduationproject.model.po.UserRolePo;
import com.syt.graduationproject.model.request.LoginRequest;
import com.syt.graduationproject.model.request.RegisterRequest;
import com.syt.graduationproject.model.request.UserInfoUpdateRequest;
import com.syt.graduationproject.model.request.UserPasswordUpdateRequest;
import com.syt.graduationproject.model.vo.LoginVo;
import com.syt.graduationproject.model.vo.UserInfoVo;
import com.syt.graduationproject.repository.InteractRepository;
import com.syt.graduationproject.repository.UserRepository;
import com.syt.graduationproject.repository.VideoRepository;
import com.syt.graduationproject.service.InteractService;
import com.syt.graduationproject.service.minio.MinioService;
import com.syt.graduationproject.service.UserService;
import com.syt.graduationproject.util.JwtUtil;
import com.syt.graduationproject.util.RedisKeyUtil;
import com.syt.graduationproject.util.UserHolderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.syt.graduationproject.constant.UserConstant.*;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final InteractService interactService;

    private final UserRepository userRepository;

    private final VideoRepository videoRepository;

    private final InteractRepository interactRepository;

    private final UserMapper userMapper;

    private final UserRoleMapper userRoleMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final MinioService minioService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request) {
        Long account = request.getAccount();
        String password = request.getPassword();
        String username = request.getUsername();

        // 判断用户是否存在
        UserPo userPo = userRepository.queryUserByAccount(account);
        if (Objects.nonNull(userPo)) {
            log.warn("用户重复注册，用户账号：{}", account);
            throw new ErrorOperationException("该账号已经注册");
        }

        // 添加用户
        UserPo newUser = UserPo.builder()
                .account(account)
                .password(password)
                .username(username)
                .avatarUrl(DEFAULT_AVATAR)
                .bio(DEFAULT_BIO)
                .build();
        userMapper.insert(newUser);

        UserRolePo userRolePo = UserRolePo.builder()
                .userId(newUser.getId())
                .roleId(RoleEnum.USER.getRoleId())
                .build();
        userRoleMapper.insert(userRolePo);

        // 初始化用户数据统计信息
        interactRepository.initUserStats(newUser.getId());
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

        // 校验密码
        if (!userPo.getPassword().equals(password)) {
            throw new ErrorParamException("账号或密码错误");
        }

        // 生成 JwtToken
        Map<String, Object> claimMap = new HashMap<>();
        Long userId = userPo.getId();
        Long roleId = RoleEnum.USER.getRoleId();
        UserRolePo userRolePo = userRoleMapper.selectOne(new QueryWrapper<UserRolePo>()
                .lambda()
                .eq(UserRolePo::getUserId, userId));
        if (userRolePo != null && userRolePo.getRoleId() != null) {
            roleId = userRolePo.getRoleId();
        }
        RoleEnum roleEnum = RoleEnum.fromRoleId(roleId);
        String roleCode = roleEnum == null ? RoleEnum.USER.getRoleCode() : roleEnum.getRoleCode();
        claimMap.put(USER_ID, userId);
        claimMap.put(USERNAME, userPo.getUsername());
        claimMap.put(ROLE_ID, roleId);
        claimMap.put(ROLE_CODE, roleCode);
        String jwtToken = JwtUtil.generateJwtToken(claimMap);
        log.info("用户登录成功，用户ID：{}，用户名：{}，JWT令牌：{}", userId, userPo.getUsername(), jwtToken);

        // 加入 Redis 白名单
        stringRedisTemplate.opsForValue()
                .set(RedisKeyUtil.jwtWhitelistKey(userId), jwtToken, JwtUtil.EXPIRATION, TimeUnit.MILLISECONDS);
        interactService.claimDailyCoin(userId);
        return LoginVo.builder()
                .userId(userId)
                .username(userPo.getUsername())
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
        if (!Objects.equals(myId, userId) && interactService.hasMutualBlock(myId, userId)) {
            throw new ErrorOperationException("由于隐私设置，无法查看该用户主页");
        }
        // 查询用户是否存在
        UserPo userPo = userRepository.queryUserById(userId);
        if (Objects.isNull(userPo)) {
            log.warn("用户不存在，用户ID：{}", userId);
            throw new ErrorOperationException("用户不存在");
        }

        // 判断用户状态
        Integer status = userPo.getStatus();
        if (status.equals(UserStatusEnum.DELETED.getCode())) {
            log.warn("用户已删除，用户ID：{}", userId);
            throw new ErrorOperationException("用户已删除");
        }
        UserInfoVo userInfoVo = UserInfoVo.builder()
                .userId(userPo.getId())
                .username(userPo.getUsername())
                .avatarUrl(userPo.getAvatarUrl())
                .bio(userPo.getBio())
                .isBanned(userPo.getStatus().equals(UserStatusEnum.BANNED.getCode()))
                .build();

        // 查询关注信息
        FollowBo followBo = interactService.queryFollow(myId, userId);
        userInfoVo.setIsFollow(followBo.getIsFollow());
        userInfoVo.setIsFans(followBo.getIsFans());

        // 查询各种数量
        userInfoVo.setVideoNum(videoRepository.queryUserVideoNum(userId));
        userInfoVo.setFansNum(interactRepository.queryUserFansNum(userId));
        userInfoVo.setFollowNum(interactRepository.queryUserFollowNum(userId));
        userInfoVo.setLikeNum(interactRepository.queryUserLikeNum(userId));
        userInfoVo.setIsBlack(interactService.hasMutualBlock(myId, userId));

        log.info("查询用户信息成功，用户ID：{}，用户信息：{}", userId, userInfoVo);
        return userInfoVo;
    }

    @Override
    public void updateUserInfo(Long userId, UserInfoUpdateRequest request) {
        UserPo userPo = userRepository.queryUserById(userId);
        if (userPo == null) {
            throw new ErrorOperationException("用户不存在");
        }
        boolean changed = false;
        if (request.getUsername() != null) {
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
        }
    }

    @Override
    public String uploadAvatar(MultipartFile file) {
        try {
            // 头像上传到 avatars 文件夹
            return minioService.uploadFile(file, "avatars");
        } catch (Exception e) {
            log.error("上传头像到Minio失败", e);
            throw new ErrorOperationException("上传头像失败");
        }
    }

    @Override
    public void logout(Long userId) {
        stringRedisTemplate.delete(RedisKeyUtil.jwtWhitelistKey(userId));
    }

    @Override
    public void updatePassword(Long userId, UserPasswordUpdateRequest request) {
        UserPo userPo = userRepository.queryUserById(userId);
        if (userPo == null) {
            throw new ErrorOperationException("用户不存在");
        }
        if (!userPo.getPassword().equals(request.getOldPassword())) {
            throw new ErrorParamException("原密码错误");
        }
        userPo.setPassword(request.getNewPassword());
        userMapper.updateById(userPo);
    }
}
