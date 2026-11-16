package com.scut.conference.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 论文。对应数据库列 abstract(Java 关键字,故字段名为 paperAbstract,
 * Mapper 查询中统一用 abstract AS "paperAbstract" 别名映射)
 */
@Data
public class Paper {
    private Long id;
    private String title;
    private String titleEn;
    private String paperAbstract;
    private String abstractEn;
    private String keywords;
    private String keywordsEn;
    private Long topicId;
    private Long submitterId;
    private String status;
    private Boolean reviewPlanPublished;
    private LocalDateTime deletedAt;
    @JsonIgnore
    private String filePath;
    @JsonIgnore
    private String finalFilePath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @JsonProperty("hasMainFile")
    public boolean hasMainFile() { return filePath != null && !filePath.isBlank(); }

    @JsonProperty("hasFinalFile")
    public boolean hasFinalFile() { return finalFilePath != null && !finalFilePath.isBlank(); }
}
