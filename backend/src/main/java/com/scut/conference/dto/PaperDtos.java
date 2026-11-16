package com.scut.conference.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** 投稿相关请求对象 */
public class PaperDtos {

    public record AuthorItem(
            @NotBlank(message = "作者姓名不能为空")
            String name,
            String pinyin,
            String email,
            String affiliation,
            String postalCode,
            boolean isCorresponding) {}

    /** 投稿请求:与上传文件一起以 multipart 的 paper 部分(JSON 字符串)提交 */
    public record PaperSubmitRequest(
            @NotBlank(message = "论文标题不能为空")
            String title,
            String titleEn,
            @NotBlank(message = "论文摘要不能为空")
            String abstractText,
            String abstractEn,
            @NotBlank(message = "关键词不能为空")
            String keywords,
            String keywordsEn,
            @NotNull(message = "请选择所属专题")
            Long topicId,
            @NotEmpty(message = "请至少添加一位作者")
            List<AuthorItem> authors) {}
}
