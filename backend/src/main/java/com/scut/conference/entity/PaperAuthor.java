package com.scut.conference.entity;

import lombok.Data;

/** 论文作者(多作者,sort_no 维护排序) */
@Data
public class PaperAuthor {
    private Long id;
    private Long paperId;
    private String name;
    private String pinyin;
    private String email;
    private String affiliation;
    private String postalCode;
    private Integer sortNo;
    private Boolean isCorresponding;
}
