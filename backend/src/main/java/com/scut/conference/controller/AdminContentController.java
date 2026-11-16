package com.scut.conference.controller;

import com.scut.conference.common.Result;
import com.scut.conference.dto.AdminDtos.ConferenceRequest;
import com.scut.conference.dto.AdminDtos.KeynoteRequest;
import com.scut.conference.dto.AdminDtos.MaterialMetaRequest;
import com.scut.conference.dto.AdminDtos.NotificationRequest;
import com.scut.conference.dto.AdminDtos.ScheduleRequest;
import com.scut.conference.dto.AdminDtos.TopicRequest;
import com.scut.conference.security.RequireRole;
import com.scut.conference.service.ContentService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 管理后台:内容管理(专题/主旨报告/日程/通知/资料/会议信息/统计) */
@RestController
@RequestMapping("/api/admin")
@RequireRole("ADMIN")
public class AdminContentController {

    private final ContentService contentService;

    public AdminContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping("/stats")
    public Result<?> stats() {
        return Result.ok(contentService.adminStats());
    }

    // ---- 专题 ----
    @PostMapping("/topic")
    public Result<Long> createTopic(@Valid @RequestBody TopicRequest req) {
        return Result.ok(contentService.createTopic(req));
    }

    @PutMapping("/topic/{id}")
    public Result<Void> updateTopic(@PathVariable Long id, @Valid @RequestBody TopicRequest req) {
        contentService.updateTopic(id, req);
        return Result.ok();
    }

    @PostMapping(value = "/topic/{id}/ppt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Void> uploadTopicPpt(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        contentService.uploadTopicPpt(id, file);
        return Result.ok();
    }

    @DeleteMapping("/topic/{id}")
    public Result<Void> deleteTopic(@PathVariable Long id) {
        contentService.deleteTopic(id);
        return Result.ok();
    }

    // ---- 主旨报告 ----
    @PostMapping("/keynote")
    public Result<Void> createKeynote(@Valid @RequestBody KeynoteRequest req) {
        contentService.createKeynote(req);
        return Result.ok();
    }

    @PutMapping("/keynote/{id}")
    public Result<Void> updateKeynote(@PathVariable Long id, @Valid @RequestBody KeynoteRequest req) {
        contentService.updateKeynote(id, req);
        return Result.ok();
    }

    @DeleteMapping("/keynote/{id}")
    public Result<Void> deleteKeynote(@PathVariable Long id) {
        contentService.deleteKeynote(id);
        return Result.ok();
    }

    // ---- 日程 ----
    @PostMapping("/schedule")
    public Result<Void> createSchedule(@Valid @RequestBody ScheduleRequest req) {
        contentService.createSchedule(req);
        return Result.ok();
    }

    @PutMapping("/schedule/{id}")
    public Result<Void> updateSchedule(@PathVariable Long id, @Valid @RequestBody ScheduleRequest req) {
        contentService.updateSchedule(id, req);
        return Result.ok();
    }

    @DeleteMapping("/schedule/{id}")
    public Result<Void> deleteSchedule(@PathVariable Long id) {
        contentService.deleteSchedule(id);
        return Result.ok();
    }

    // ---- 通知 ----
    @GetMapping("/notifications")
    public Result<?> notifications() {
        return Result.ok(contentService.listAllNotifications());
    }

    @PostMapping("/notification")
    public Result<Long> createNotification(@Valid @RequestBody NotificationRequest req) {
        return Result.ok(contentService.createNotification(req));
    }

    @PutMapping("/notification/{id}")
    public Result<Void> updateNotification(@PathVariable Long id, @Valid @RequestBody NotificationRequest req) {
        contentService.updateNotification(id, req);
        return Result.ok();
    }

    @PostMapping(value = "/notification/{id}/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Void> uploadNotificationPdf(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        contentService.uploadNotificationPdf(id, file);
        return Result.ok();
    }

    @PostMapping("/notification/{id}/publish")
    public Result<Void> publishNotification(@PathVariable Long id) {
        contentService.publishNotification(id);
        return Result.ok();
    }

    @DeleteMapping("/notification/{id}")
    public Result<Void> deleteNotification(@PathVariable Long id) {
        contentService.deleteNotification(id);
        return Result.ok();
    }

    // ---- 资料 ----
    @PostMapping(value = "/material", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Void> uploadMaterial(@RequestPart("file") MultipartFile file,
                                       @RequestPart(value = "meta", required = false) MaterialMetaRequest meta) {
        contentService.uploadMaterial(file, meta == null ? new MaterialMetaRequest(null, null) : meta);
        return Result.ok();
    }

    @PutMapping("/material/{id}")
    public Result<Void> updateMaterial(@PathVariable Long id, @RequestBody MaterialMetaRequest req) {
        contentService.updateMaterialMeta(id, req);
        return Result.ok();
    }

    @DeleteMapping("/material/{id}")
    public Result<Void> deleteMaterial(@PathVariable Long id) {
        contentService.deleteMaterial(id);
        return Result.ok();
    }

    // ---- 会议信息 ----
    @PutMapping("/conference")
    public Result<Void> updateConference(@RequestBody ConferenceRequest req) {
        contentService.updateConference(req);
        return Result.ok();
    }
}
