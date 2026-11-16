package com.scut.conference.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 会议资料(可下载文件) */
@Data
public class Material {
    private Long id;
    private String name;
    private String description;
    private String filePath;
    private Long fileSize;
    private Integer downloadCount;
    private LocalDateTime createdAt;
}
