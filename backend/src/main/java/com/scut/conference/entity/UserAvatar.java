package com.scut.conference.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 用户头像(BYTEA 大对象,与用户表垂直拆分) */
@Data
public class UserAvatar {
    private Long userId;
    private byte[] content;
    private String contentType;
    private LocalDateTime updatedAt;
}
