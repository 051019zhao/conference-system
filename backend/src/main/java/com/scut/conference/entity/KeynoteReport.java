package com.scut.conference.entity;

import lombok.Data;

/** 主旨报告 */
@Data
public class KeynoteReport {
    private Long id;
    /** 对应 expert_profile.id */
    private Long expertId;
    private String title;
    private String paperAbstract;
    private String timeNote;
    private Integer sortNo;
}
