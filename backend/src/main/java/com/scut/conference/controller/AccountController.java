package com.scut.conference.controller;

import com.scut.conference.common.Result;
import com.scut.conference.dto.AccountDtos.ChangePasswordRequest;
import com.scut.conference.dto.AccountDtos.UpdateProfileRequest;
import com.scut.conference.dto.AccountDtos.ExpertProfileRequest;
import com.scut.conference.security.UserContext;
import com.scut.conference.service.AuthService;
import com.scut.conference.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 个人中心(任意登录角色可用) */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AuthService authService;
    private final UserAdminService userAdminService;

    public AccountController(AuthService authService, UserAdminService userAdminService) {
        this.authService = authService;
        this.userAdminService = userAdminService;
    }

    /** 修改姓名/邮箱 */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest req) {
        authService.updateProfile(UserContext.requireUserId(), req);
        return Result.ok();
    }

    @org.springframework.web.bind.annotation.GetMapping("/expert-profile")
    public Result<?> expertProfile() {
        return Result.ok(userAdminService.myExpertProfile());
    }

    @PutMapping("/expert-profile")
    public Result<Void> updateExpertProfile(@RequestBody ExpertProfileRequest req) {
        userAdminService.updateOwnExpertProfile(req);
        return Result.ok();
    }

    /** 修改密码(验证原密码) */
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        authService.changePassword(UserContext.requireUserId(), req.oldPassword(), req.newPassword());
        return Result.ok();
    }

    /** 上传/更换头像(PNG/JPEG,≤2MB,存 openGauss BYTEA) */
    @PostMapping(value = "/avatar", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Void> uploadAvatar(@org.springframework.web.bind.annotation.RequestPart("file")
                                     MultipartFile file) {
        authService.saveAvatar(UserContext.requireUserId(), file);
        return Result.ok();
    }
}
