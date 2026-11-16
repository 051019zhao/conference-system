package com.scut.conference.controller;

import com.scut.conference.common.Result;
import com.scut.conference.dto.AdminDtos.CreateUserRequest;
import com.scut.conference.dto.AdminDtos.UpdateUserRequest;
import com.scut.conference.security.RequireRole;
import com.scut.conference.service.ContentService;
import com.scut.conference.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/** 管理后台:用户管理 */
@RestController
@RequestMapping("/api/admin")
@RequireRole("ADMIN")
public class AdminUserController {

    private final UserAdminService userAdminService;
    private final ContentService contentService;

    public AdminUserController(UserAdminService userAdminService, ContentService contentService) {
        this.userAdminService = userAdminService;
        this.contentService = contentService;
    }

    @GetMapping("/users")
    public Result<?> users(@RequestParam(required = false) String role,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return Result.ok(userAdminService.listUsers(role, keyword, Math.max(page, 1), safeSize));
    }

    /** 创建账号,返回系统生成的用户名/密码(仅此一次展示) */
    @PostMapping("/user")
    public Result<?> create(@Valid @RequestBody CreateUserRequest req) {
        return Result.ok(userAdminService.create(req));
    }

    @PostMapping(value = "/experts/import", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<?> importExperts(@RequestPart("file") MultipartFile file) {
        return Result.ok(userAdminService.importExperts(file));
    }

    @PutMapping("/user/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody UpdateUserRequest req) {
        userAdminService.update(id, req);
        return Result.ok();
    }

    @PutMapping("/user/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        userAdminService.toggleStatus(id, status);
        return Result.ok();
    }

    /** 注册审核:通过(approve=true)/拒绝(approve=false),仅待审核账号可操作 */
    @PutMapping("/user/{id}/review")
    public Result<Void> reviewRegister(@PathVariable Long id, @RequestParam boolean approve) {
        userAdminService.reviewRegister(id, approve);
        return Result.ok();
    }

    /** 重置密码,返回一次性新密码 */
    @PutMapping("/user/{id}/password")
    public Result<?> resetPassword(@PathVariable Long id) {
        return Result.ok(java.util.Map.of("password", userAdminService.resetPassword(id)));
    }

    /** 精简专家列表(评审分配、主旨报告选择用) */
    @GetMapping("/experts-lite")
    public Result<?> expertsLite() {
        return Result.ok(contentService.listExpertsLite());
    }
}
