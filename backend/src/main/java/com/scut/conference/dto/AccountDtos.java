package com.scut.conference.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 个人中心 / 找回密码相关请求对象 */
public class AccountDtos {

    public record UpdateProfileRequest(
            @NotBlank(message = "姓名不能为空") String realName,
            @NotBlank(message = "邮箱不能为空") @Email(message = "邮箱格式不正确") String email,
            String gender, String education, String jobTitle, String phone,
            String workUnit, String postalCode) {}

    public record ExpertProfileRequest(String title, String affiliation, String bio,
                                       String address, String website, String researchKeywords,
                                       String reviewTopics, Integer reviewCapacity,
                                       Boolean reviewEnabled) {}

    public record ChangePasswordRequest(
            @NotBlank(message = "请输入原密码") String oldPassword,
            @NotBlank(message = "请输入新密码")
            @Size(min = 6, max = 20, message = "新密码长度需为 6-20 位") String newPassword) {}

    public record ForgotPasswordRequest(
            @NotBlank(message = "邮箱不能为空") @Email(message = "邮箱格式不正确") String email,
            @NotBlank(message = "验证码不能为空") String code,
            @NotBlank(message = "请输入新密码")
            @Size(min = 6, max = 20, message = "新密码长度需为 6-20 位") String newPassword) {}
}
