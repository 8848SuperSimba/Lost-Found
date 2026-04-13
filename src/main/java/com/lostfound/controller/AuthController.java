package com.lostfound.controller;

import com.lostfound.common.Result;
import com.lostfound.common.ResultCode;
import com.lostfound.dto.LoginRequest;
import com.lostfound.dto.RegisterRequest;
import com.lostfound.service.UserService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(userService.login(request));
    }

    @PostMapping("/wx-login")
    public Result<Void> wxLogin() {
        return Result.fail(ResultCode.ERROR, "微信登录暂未开放");
    }
}
