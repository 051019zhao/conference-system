package com.scut.conference.controller;

import com.scut.conference.common.DownloadFile;
import com.scut.conference.common.Result;
import com.scut.conference.service.ContentService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/** 前台公开接口(匿名访问) */
@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final ContentService contentService;
    private final com.scut.conference.service.PaperService paperService;
    private final com.scut.conference.service.AuthService authService;

    public PublicController(ContentService contentService,
                            com.scut.conference.service.PaperService paperService,
                            com.scut.conference.service.AuthService authService) {
        this.contentService = contentService;
        this.paperService = paperService;
        this.authService = authService;
    }

    /** 用户头像(匿名可读,<img> 标签无法携带 Token;无头像返回 404 由前端回退为文字头像) */
    @GetMapping("/avatar/{userId}")
    public ResponseEntity<byte[]> avatar(@PathVariable Long userId) {
        com.scut.conference.entity.UserAvatar avatar = authService.getAvatar(userId);
        if (avatar == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(avatar.getContentType()))
                .cacheControl(org.springframework.http.CacheControl.maxAge(java.time.Duration.ofMinutes(10)))
                .body(avatar.getContent());
    }

    /** 录用论文列表(会议论文集) */
    @GetMapping("/accepted-papers")
    public Result<?> acceptedPapers() {
        return Result.ok(paperService.listAcceptedPublic());
    }

    /** 录用论文公开下载;仅已录用/已交终稿状态允许访问。 */
    @GetMapping("/accepted-paper/{id}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadAcceptedPaper(@PathVariable Long id) {
        return downloadResponse(paperService.downloadAcceptedPublic(id));
    }

    @GetMapping("/conference")
    public Result<?> conference() {
        return Result.ok(contentService.conferenceInfo());
    }

    @GetMapping("/topic/{id}/ppt")
    public ResponseEntity<org.springframework.core.io.Resource> downloadTopicPpt(@PathVariable Long id) {
        return downloadResponse(contentService.downloadTopicPpt(id));
    }

    @GetMapping("/experts")
    public Result<?> experts() {
        return Result.ok(contentService.listPublicExperts());
    }

    @GetMapping("/topics")
    public Result<?> topics() {
        return Result.ok(contentService.listTopics());
    }

    @GetMapping("/keynotes")
    public Result<?> keynotes() {
        return Result.ok(contentService.listKeynotes());
    }

    @GetMapping("/schedule")
    public Result<?> schedule() {
        return Result.ok(contentService.listSchedule());
    }

    @GetMapping("/notifications")
    public Result<?> notifications() {
        return Result.ok(contentService.listPublishedNotifications());
    }

    @GetMapping("/notification/{id}")
    public Result<?> notificationDetail(@PathVariable Long id) {
        return Result.ok(contentService.publishedDetail(id));
    }

    @GetMapping("/notification/{id}/pdf")
    public ResponseEntity<org.springframework.core.io.Resource> downloadNotificationPdf(@PathVariable Long id) {
        return downloadResponse(contentService.downloadNotificationPdf(id));
    }

    @GetMapping("/materials")
    public Result<?> materials() {
        return Result.ok(contentService.listMaterials());
    }

    /** 资料下载(下载量 +1) */
    @GetMapping("/material/{id}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadMaterial(@PathVariable Long id) {
        DownloadFile file = contentService.downloadMaterial(id);
        return downloadResponse(file);
    }

    @GetMapping("/stats")
    public Result<?> stats() {
        return Result.ok(contentService.publicStats());
    }

    static ResponseEntity<org.springframework.core.io.Resource> downloadResponse(DownloadFile file) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8).build());
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file.resource());
    }
}
