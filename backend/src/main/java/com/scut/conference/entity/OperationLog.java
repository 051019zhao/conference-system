package com.scut.conference.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 操作审计日志 */
@Data
public class OperationLog {
    private Long id;
    private Long userId;
    private String username;
    private String role;
    private String action;
    private String detail;
    private String ip;
    private LocalDateTime createdAt;
}
