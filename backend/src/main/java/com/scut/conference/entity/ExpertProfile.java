package com.scut.conference.entity;

import lombok.Data;

/** 专家档案 */
@Data
public class ExpertProfile {
    private Long id;
    private Long userId;
    private String title;
    private String affiliation;
    private String bio;
    private String researchKeywords;
    private String photoUrl;
    private String address;
    private String website;
    private String reviewTopics;
    private Integer reviewCapacity;
    private Boolean reviewEnabled;
    private Boolean isKeynote;
}
