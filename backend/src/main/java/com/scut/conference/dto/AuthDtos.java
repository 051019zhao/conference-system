package com.scut.conference.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 认证相关请求对象 */
public class AuthDtos {

    public record SendCodeRequest(
            @NotBlank(message = "邮箱不能为空")
            @Email(message = "邮箱格式不正确")
            String email) {}

    public record RegisterRequest(
            @NotBlank(message = "用户名不能为空")
            @Size(min = 3, max = 20, message = "用户名长度需为 3-20 位")
            String username,
            @NotBlank(message = "姓名不能为空")
            String realName,
            @NotBlank(message = "邮箱不能为空")
            @Email(message = "邮箱格式不正确")
            String email,
            @NotBlank(message = "密码不能为空")
            @Size(min = 6, max = 20, message = "密码长度需为 6-20 位")
            String password,
            @NotBlank(message = "验证码不能为空")
            String code) {}

    public record LoginRequest(
            @NotBlank(message = "用户名不能为空")
            String username,
            @NotBlank(message = "密码不能为空")
            String password) {}
}
