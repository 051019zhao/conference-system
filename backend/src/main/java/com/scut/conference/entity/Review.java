package com.scut.conference.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 评审记录(专家对一篇论文的唯一评审) */
@Data
public class Review {
    private Long id;
    private Long paperId;
    private Long expertId;
    /** PENDING 待评审 / SUBMITTED 已提交 */
    private String status;
    private String overallComment;
    private String revisionComment;
    /** 评审表五维评分(1-5):契合度/创新性/完整性/深度/表达 */
    private Integer scoreFit;
    private Integer scoreInnovation;
    private Integer scoreComplete;
    private Integer scoreDepth;
    private Integer scoreClarity;
    /** ACCEPT 建议录用 / REJECT 建议退稿 */
    private String result;
    private Boolean recommendJournal;
    private String rubricJson;
    private String recommendationGrade;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime submittedAt;
}
