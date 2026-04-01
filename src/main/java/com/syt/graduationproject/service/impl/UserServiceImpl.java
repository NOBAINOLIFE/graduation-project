package com.syt.graduationproject.service.impl;

import com.syt.graduationproject.exception.ErrorOperationException;
import com.syt.graduationproject.exception.ErrorParamException;
import com.syt.graduationproject.mapper.UserMapper;
import com.syt.graduationproject.model.po.UserPo;
import com.syt.graduationproject.model.request.LoginRequest;
import com.syt.graduationproject.model.request.RegisterRequest;
import com.syt.graduationproject.model.vo.LoginVo;
import com.syt.graduationproject.repository.UserRepository;
import com.syt.graduationproject.service.UserService;
import com.syt.graduationproject.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    private final UserMapper userMapper;

    private final UserRepository userRepository;

    @Override
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

        return LoginVo.builder()
                .userId(userId)
                .username(userPo.getUsername())
                .token(jwtToken)
                .build();
    }
}
