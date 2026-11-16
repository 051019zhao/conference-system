package com.scut.conference.entity;

import lombok.Data;

import java.time.LocalDate;

/** 会议日程条目 */
@Data
public class ScheduleItem {
    private Long id;
    private LocalDate day;
    private String startTime;
    private String endTime;
    private String title;
    private String speaker;
    private String location;
    private String type;
    private Integer sortNo;
}
