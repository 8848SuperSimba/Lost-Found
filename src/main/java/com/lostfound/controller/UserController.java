package com.lostfound.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lostfound.common.Result;
import com.lostfound.common.ResultCode;
import com.lostfound.dto.ChangePasswordRequest;
import com.lostfound.dto.UpdateUserRequest;
import com.lostfound.enums.UserStatus;
import com.lostfound.exception.BusinessException;
import com.lostfound.service.UserService;
import com.lostfound.vo.UserVO;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api/user/me")
    public Result<UserVO> getCurrentUser() {
        return Result.success(userService.getCurrentUser(getCurrentUserId()));
    }

    @PutMapping("/api/user/me")
    public Result<Void> updateCurrentUser(@Valid @RequestBody UpdateUserRequest request) {
        userService.updateCurrentUser(getCurrentUserId(), request);
        return Result.success();
    }

    @PutMapping("/api/user/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(getCurrentUserId(), request);
        return Result.success();
    }

    @GetMapping("/api/admin/users")
    public Result<IPage<UserVO>> adminListUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(userService.adminListUsers(keyword, status, page, size));
    }

    @PutMapping("/api/admin/users/{id}/ban")
    public Result<Void> adminUpdateUserStatus(@PathVariable("id") Long id, @RequestBody Map<String, String> request) {
        String statusValue = request.get("status");
        if (statusValue == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "status 不能为空");
        }

        UserStatus status;
        try {
            status = UserStatus.valueOf(statusValue.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "status 仅支持 ACTIVE 或 BANNED");
        }

        userService.adminUpdateUserStatus(getCurrentUserId(), id, status);
        return Result.success();
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        if (principal instanceof String principalString) {
            try {
                return Long.valueOf(principalString);
            } catch (NumberFormatException ex) {
                throw new BusinessException(ResultCode.UNAUTHORIZED);
            }
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED);
    }
}
