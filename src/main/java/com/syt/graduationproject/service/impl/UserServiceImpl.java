package com.syt.graduationproject.service.impl;

import com.syt.graduationproject.enums.UserStatusEnum;
import com.syt.graduationproject.exception.ErrorOperationException;
import com.syt.graduationproject.exception.ErrorParamException;
import com.syt.graduationproject.mapper.UserMapper;
import com.syt.graduationproject.model.bo.FollowBo;
import com.syt.graduationproject.model.po.UserPo;
import com.syt.graduationproject.model.request.LoginRequest;
import com.syt.graduationproject.model.request.RegisterRequest;
import com.syt.graduationproject.model.vo.LoginVo;
import com.syt.graduationproject.model.vo.UserInfoVo;
import com.syt.graduationproject.repository.InteractRepository;
import com.syt.graduationproject.repository.UserRepository;
import com.syt.graduationproject.repository.VideoRepository;
import com.syt.graduationproject.service.InteractService;
import com.syt.graduationproject.service.UserService;
import com.syt.graduationproject.util.JwtUtil;
import com.syt.graduationproject.util.UserHolderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

        // 生成JwtToken
        Map<String, Object> claimMap = new HashMap<>();
        Long userId = userPo.getId();
        claimMap.put(USER_ID, userId);
        claimMap.put(USERNAME, userPo.getUsername());
        String jwtToken = JwtUtil.generateJwtToken(claimMap);
        log.info("用户登录成功，用户ID：{}，用户名：{}，JWT令牌：{}", userId, userPo.getUsername(), jwtToken);

        return LoginVo.builder()
                .userId(userId)
                .username(userPo.getUsername())
                .token(jwtToken)
                .build();
    }

    /**
     * 查询用户信息
     */
    @Override
    public UserInfoVo queryUserInfo(Long userId) {
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
        Long myId = UserHolderUtil.getUser().getUserId();
        FollowBo followBo = interactService.queryFollow(myId, userId);
        userInfoVo.setIsFollow(followBo.getIsFollow());
        userInfoVo.setIsFans(followBo.getIsFans());

        // 查询各种数量
        userInfoVo.setVideoNum(videoRepository.queryUserVideoNum(myId));
        userInfoVo.setFansNum(interactRepository.queryUserFansNum(myId));
        userInfoVo.setFollowNum(interactRepository.queryUserFollowNum(myId));
        userInfoVo.setLikeNum(interactRepository.queryUserLikeNum(myId));

        // TODO 添加黑名单
        userInfoVo.setIsBlack(false);

        log.info("查询用户信息成功，用户ID：{}，用户信息：{}", userId, userInfoVo);
        return userInfoVo;
    }
}
