package com.scut.conference.controller;

import com.scut.conference.common.Result;
import com.scut.conference.dto.AdminDtos.AssignRequest;
import com.scut.conference.dto.AdminDtos.DecideRequest;
import com.scut.conference.dto.AdminDtos.PublishReviewPlansRequest;
import com.scut.conference.security.RequireRole;
import com.scut.conference.service.PaperService;
import com.scut.conference.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 管理后台:论文管理(状态流转 + 评审分配) */
@RestController
@RequestMapping("/api/admin")
@RequireRole("ADMIN")
public class AdminPaperController {

    private final PaperService paperService;
    private final ReviewService reviewService;

    public AdminPaperController(PaperService paperService, ReviewService reviewService) {
        this.paperService = paperService;
        this.reviewService = reviewService;
    }

    @GetMapping("/papers")
    public Result<?> papers(@RequestParam(required = false) String status,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(paperService.adminPapers(status, keyword, Math.max(page, 1), clamp(size)));
    }

    /** 导出论文列表 CSV(Excel 可直接打开,UTF-8 带 BOM) */
    @SuppressWarnings("unchecked")
    @GetMapping("/papers/export")
    public org.springframework.http.ResponseEntity<byte[]> exportPapers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        Map<String, Object> pageData = paperService.adminPapers(status, keyword, 1, 100000);
        List<Map<String, Object>> papers = (List<Map<String, Object>>) pageData.get("list");
        Map<String, String> statusText = Map.of(
                "DRAFT", "草稿", "SUBMITTED", "待初审", "REVIEWING", "外审中", "ACCEPTED", "已录用",
                "REJECTED", "已退稿", "FINAL_SUBMITTED", "终稿已提交", "WITHDRAWN", "已撤回");
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append("编号,论文标题,投稿人,专题,状态,关键词,已提交评审/分配专家,投稿时间\n");
        for (Map<String, Object> p : papers) {
            String paperStatus = String.valueOf(p.get("status"));
            boolean returned = "REVIEWING".equals(paperStatus)
                    && Boolean.TRUE.equals(p.get("reviewPlanPublished"))
                    && ((Number) p.get("reviewCount")).intValue() > 0
                    && ((Number) p.get("reviewCount")).intValue()
                    == ((Number) p.get("submittedCount")).intValue();
            sb.append(p.get("id")).append(',')
                    .append(csv(p.get("title"))).append(',')
                    .append(csv(p.get("submitterName"))).append(',')
                    .append(csv(p.get("topicName"))).append(',')
                    .append(csv(returned ? "评审意见已返回" : statusText.get(paperStatus))).append(',')
                    .append(csv(p.get("keywords"))).append(',')
                    .append(p.get("submittedCount")).append('/').append(p.get("reviewCount")).append(',')
                    .append(csv(String.valueOf(p.get("createdAt")).replace("T", " "))).append('\n');
        }
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=papers.csv")
                .contentType(new org.springframework.http.MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String csv(Object value) {
        String s = value == null ? "" : String.valueOf(value);
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }

    private int clamp(int size) {
        return Math.min(Math.max(size, 1), 100);
    }

    @GetMapping("/paper/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.ok(paperService.detail(id));
    }

    /** 初审通过,进入外审 */
    @PostMapping("/paper/{id}/first-pass")
    public Result<Void> firstPass(@PathVariable Long id) {
        paperService.firstPass(id);
        return Result.ok();
    }

    /** 退稿 */
    @PostMapping("/paper/{id}/reject")
    public Result<Void> reject(@PathVariable Long id) {
        paperService.reject(id);
        return Result.ok();
    }

    /** 手动分配评审专家 */
    @PostMapping("/paper/{id}/assign")
    public Result<Void> assign(@PathVariable Long id, @Valid @RequestBody AssignRequest req) {
        reviewService.assign(id, req.expertIds());
        return Result.ok();
    }

    /** 按关键词自动分配,返回匹配到的专家 */
    @PostMapping("/paper/{id}/auto-assign")
    public Result<?> autoAssign(@PathVariable Long id) {
        return Result.ok(reviewService.autoAssign(id));
    }

    /** 管理员检查或调整方案后批量发布；不足两名专家的论文返回提醒且不会发布。 */
    @PostMapping("/review-plans/publish")
    public Result<?> publishReviewPlans(@Valid @RequestBody PublishReviewPlansRequest req) {
        return Result.ok(reviewService.publishPlans(req.paperIds()));
    }

    /** 终审录用/退稿 */
    @PostMapping("/paper/{id}/decide")
    public Result<Void> decide(@PathVariable Long id, @Valid @RequestBody DecideRequest req) {
        paperService.decide(id, req.result());
        return Result.ok();
    }
}
