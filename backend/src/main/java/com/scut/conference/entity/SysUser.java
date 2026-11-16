package com.scut.conference.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/** 系统用户(管理员/专家/作者) */
@Data
public class SysUser {
    private Long id;
    private String username;
    /** BCrypt 哈希,任何接口都不返回给前端 */
    @JsonIgnore
    private String passwordHash;
    private String realName;
    private String email;
    private String gender;
    private String education;
    private String jobTitle;
    private String phone;
    private String workUnit;
    private String postalCode;
    private String role;
    private Integer status;
    private LocalDateTime createdAt;
}
