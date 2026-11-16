package com.scut.conference.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/** 论文附件(元数据账本,内容在磁盘):MAIN 主稿 / FINAL 终稿 / SUPPLEMENT 附属资料 */
@Data
public class PaperFile {
    private Long id;
    private Long paperId;
    private String fileType;
    private String fileName;
    @JsonIgnore
    private String storedPath;
    private String fileExt;
    private Long fileSize;
    private Integer versionNo;
    private Boolean isCurrent;
    private Long uploadedBy;
    private LocalDateTime createdAt;
}
