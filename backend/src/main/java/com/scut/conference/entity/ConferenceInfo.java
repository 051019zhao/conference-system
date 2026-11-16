package com.scut.conference.entity;

import lombok.Data;

import java.time.LocalDate;

/** 会议基本信息(单行,id=1) */
@Data
public class ConferenceInfo {
    private Integer id;
    private String name;
    private String shortName;
    private String venue;
    private LocalDate startDate;
    private LocalDate endDate;
    /** 投稿截止(业务规则:过期禁止投稿与草稿编辑) */
    private LocalDate submitDeadline;
    /** 录用通知日期(展示用) */
    private LocalDate notifyDeadline;
    /** 终稿截止(业务规则:过期禁止提交终稿) */
    private LocalDate finalDeadline;
    private String intro;
    private String cfpText;
    private String contactEmail;
    private String organizerName;
    private String organizerUrl;
    private String organizerLogoUrl;
    private String coorganizerName;
    private String coorganizerUrl;
    private String coorganizerLogoUrl;
}
