package com.lostfound.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lostfound.dto.ChangePasswordRequest;
import com.lostfound.dto.LoginRequest;
import com.lostfound.dto.RegisterRequest;
import com.lostfound.dto.UpdateUserRequest;
import com.lostfound.dto.WxLoginRequest;
import com.lostfound.enums.UserStatus;
import com.lostfound.vo.UserVO;
import java.util.Map;

public interface UserService {

    void register(RegisterRequest request);

    Map<String, Object> login(LoginRequest request);

    Map<String, Object> wxLogin(WxLoginRequest request);

    UserVO getCurrentUser(Long userId);

    void updateCurrentUser(Long userId, UpdateUserRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    IPage<UserVO> adminListUsers(String keyword, UserStatus status, long page, long size);

    void adminUpdateUserStatus(Long adminUserId, Long targetUserId, UserStatus status);
}
