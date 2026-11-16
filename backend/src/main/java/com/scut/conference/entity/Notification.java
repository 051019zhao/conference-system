package com.scut.conference.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/** 通知公告(草稿/已发布) */
@Data
public class Notification {
    private Long id;
    private String title;
    private String content;
    private LocalDateTime lastSavedAt;
    private LocalDateTime expiresAt;
    private String publisher;
    @JsonIgnore
    private String pdfPath;
    private Boolean isPublished;
    private LocalDateTime publishTime;
    private LocalDateTime createdAt;

    @JsonProperty("hasPdf")
    public boolean hasPdf() { return pdfPath != null && !pdfPath.isBlank(); }
}
