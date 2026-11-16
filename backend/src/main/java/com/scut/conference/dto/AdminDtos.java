package com.scut.conference.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 管理后台相关请求对象 */
public class AdminDtos {

    /** 管理员创建账号(账号密码由系统生成后返回,只需一次性展示给管理员转交本人) */
    public record CreateUserRequest(
            @NotBlank(message = "姓名不能为空") String realName,
            @NotBlank(message = "邮箱不能为空") @Email(message = "邮箱格式不正确") String email,
            @NotBlank(message = "角色不能为空") String role,   // EXPERT / AUTHOR / ADMIN
            String title,
            String affiliation,
            String researchKeywords,
            String gender, String phone, String postalCode, String address,
            String website, String bio, String reviewTopics, Integer reviewCapacity) {}

    public record UpdateUserRequest(
            String realName,
            @Email(message = "邮箱格式不正确") String email,
            String title,
            String affiliation,
            String researchKeywords,
            Boolean isKeynote,
            String gender, String phone, String postalCode, String address,
            String website, String bio, String reviewTopics, Integer reviewCapacity,
            Boolean reviewEnabled) {}

    /** 分配评审专家 */
    public record AssignRequest(@NotEmpty(message = "请选择专家") List<Long> expertIds) {}

    public record PublishReviewPlansRequest(@NotEmpty(message = "请选择论文") List<Long> paperIds) {}

    /** 终审决定:result = ACCEPTED / REJECTED */
    public record DecideRequest(@NotBlank(message = "请指定终审结果") String result) {}

    public record TopicRequest(@NotBlank(message = "专题名称不能为空") String name,
                               String description, Integer sortNo, Integer reportLimit,
                               Long keynoteExpertId, String reportName, String reportTimeNote) {}

    public record KeynoteRequest(@NotNull(message = "请选择专家") Long expertId,
                                 @NotBlank(message = "报告标题不能为空") String title,
                                 String abstractText, String timeNote, Integer sortNo) {}

    public record ScheduleRequest(@NotNull(message = "日期不能为空") LocalDate day,
                                  String startTime, String endTime,
                                  @NotBlank(message = "标题不能为空") String title,
                                  String speaker, String location, String type, Integer sortNo) {}

    public record NotificationRequest(@NotBlank(message = "通知标题不能为空") String title,
                                      String content, LocalDateTime expiresAt, String publisher) {}

    public record MaterialMetaRequest(String name, String description) {}

    public record ConferenceRequest(String name, String shortName, String venue,
                                    LocalDate startDate, LocalDate endDate,
                                    LocalDate submitDeadline, LocalDate notifyDeadline, LocalDate finalDeadline,
                                    String intro, String cfpText, String contactEmail,
                                    String organizerName, String organizerUrl, String organizerLogoUrl,
                                    String coorganizerName, String coorganizerUrl, String coorganizerLogoUrl) {}
}
