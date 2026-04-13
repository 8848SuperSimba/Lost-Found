package com.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lostfound.common.ResultCode;
import com.lostfound.dto.ChangePasswordRequest;
import com.lostfound.dto.LoginRequest;
import com.lostfound.dto.RegisterRequest;
import com.lostfound.dto.UpdateUserRequest;
import com.lostfound.entity.User;
import com.lostfound.enums.UserRole;
import com.lostfound.enums.UserStatus;
import com.lostfound.exception.BusinessException;
import com.lostfound.mapper.UserMapper;
import com.lostfound.service.UserService;
import com.lostfound.util.JwtUtil;
import com.lostfound.vo.UserVO;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JdbcTemplate jdbcTemplate;

    public UserServiceImpl(
            UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, JdbcTemplate jdbcTemplate) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "两次密码输入不一致");
        }

        long usernameCount = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (usernameCount > 0) {
            throw new BusinessException(ResultCode.USER_EXISTS, "用户名已存在");
        }

        long phoneCount = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone()));
        if (phoneCount > 0) {
            throw new BusinessException(ResultCode.USER_EXISTS, "手机号已存在");
        }

        if (StringUtils.hasText(request.getEmail())) {
            long emailCount = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail()));
            if (emailCount > 0) {
                throw new BusinessException(ResultCode.USER_EXISTS, "邮箱已存在");
            }
        }

        User user = User.builder()
                .username(request.getUsername())
                .phone(request.getPhone())
                .email(request.getEmail())
                .nickname(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        userMapper.insert(user);
    }

    @Override
    public Map<String, Object> login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getIdentifier())
                .or()
                .eq(User::getPhone, request.getIdentifier())
                .or()
                .eq(User::getEmail, request.getIdentifier())
                .last("LIMIT 1"));

        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (!StringUtils.hasText(user.getPasswordHash())
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        if (user.getStatus() == UserStatus.BANNED) {
            throw new BusinessException(ResultCode.USER_BANNED);
        }

        User updateLogin = User.builder().id(user.getId()).lastLoginAt(LocalDateTime.now()).build();
        userMapper.updateById(updateLogin);

        String token = jwtUtil.generateToken(user.getId(), user.getRole().name());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", toUserVO(user));
        return result;
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return toUserVO(user);
    }

    @Override
    public void updateCurrentUser(Long userId, UpdateUserRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        User updateUser = User.builder()
                .id(userId)
                .nickname(request.getNickname())
                .email(request.getEmail())
                .avatarUrl(request.getAvatarUrl())
                .build();
        userMapper.updateById(updateUser);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        User updatePassword = User.builder()
                .id(userId)
                .passwordHash(passwordEncoder.encode(request.getNewPassword()))
                .build();
        userMapper.updateById(updatePassword);
    }

    @Override
    public IPage<UserVO> adminListUsers(String keyword, UserStatus status, long page, long size) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(User::getCreatedAt);

        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper.like(User::getUsername, keyword).or().like(User::getPhone, keyword));
        }
        if (status != null) {
            queryWrapper.eq(User::getStatus, status);
        }

        Page<User> userPage = userMapper.selectPage(new Page<>(page, size), queryWrapper);
        List<UserVO> records = userPage.getRecords().stream().map(this::toUserVO).toList();
        Page<UserVO> resultPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    public void adminUpdateUserStatus(Long adminUserId, Long targetUserId, UserStatus status) {
        User targetUser = userMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        User updateUser = User.builder().id(targetUserId).status(status).build();
        userMapper.updateById(updateUser);

        String action = status == UserStatus.BANNED ? "BAN_USER" : "UNBAN_USER";
        String detail = "将用户状态更新为 " + status.name();
        jdbcTemplate.update(
                "INSERT INTO audit_log (admin_user_id, action, target_type, target_id, detail) VALUES (?, ?, ?, ?, ?)",
                adminUserId,
                action,
                "USER",
                targetUserId,
                detail);
    }

    private UserVO toUserVO(User user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .phone(user.getPhone())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .status(user.getStatus())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
