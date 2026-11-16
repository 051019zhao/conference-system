package com.scut.conference.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/** 评审相关请求对象 */
public class ReviewDtos {

    /** 专家提交评审表单(对照课程要求的评审表:五个评分维度 + 总评 + 结论 + 期刊推荐) */
    public record ReviewSubmitRequest(
            @NotBlank(message = "请填写总体评价")
            String overallComment,
            String revisionComment,
            @NotNull(message = "请填写评审量表") Map<String, String> rubric,
            @NotBlank(message = "请选择录用或退稿等级") String recommendationGrade,
            String rejectionReason,
            @NotBlank(message = "请选择评审结论")
            String result,                // ACCEPT / REJECT
            boolean recommendJournal) {}
}
