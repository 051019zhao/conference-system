package com.scut.conference.controller;

import com.scut.conference.common.Result;
import com.scut.conference.dto.AuthDtos.LoginRequest;
import com.scut.conference.dto.AuthDtos.RegisterRequest;
import com.scut.conference.dto.AuthDtos.SendCodeRequest;
import com.scut.conference.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/** 认证接口(匿名放行) */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 发送邮箱验证码;邮件未启用时验证码直接返回,方便本地联调 */
    @PostMapping("/send-code")
    public Result<Map<String, Object>> sendCode(@Valid @RequestBody SendCodeRequest req) {
        String code = authService.sendCode(req.email());
        Map<String, Object> data = new HashMap<>();
        data.put("devCode", code);
        return Result.ok(data);
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return Result.ok();
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(authService.login(req));
    }

    /** 忘记密码:邮箱验证码校验通过后重置密码(匿名接口) */
    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(
            @Valid @RequestBody com.scut.conference.dto.AccountDtos.ForgotPasswordRequest req) {
        authService.resetPassword(req.email(), req.code(), req.newPassword());
        return Result.ok();
    }

    /** 当前登录用户信息(需 Token) */
    @GetMapping("/me")
    public Result<?> me() {
        return Result.ok(authService.currentUser());
    }
}
