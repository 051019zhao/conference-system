package com.scut.conference.controller;

import com.scut.conference.common.Result;
import com.scut.conference.dto.ReviewDtos.ReviewSubmitRequest;
import com.scut.conference.security.RequireRole;
import com.scut.conference.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 专家评审接口 */
@RestController
@RequestMapping("/api/review")
@RequireRole("EXPERT")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /** 我的评审任务 */
    @GetMapping("/tasks")
    public Result<?> tasks() {
        return Result.ok(reviewService.myTasks());
    }

    /** 提交评审表单 */
    @PostMapping("/{reviewId}/submit")
    public Result<Void> submit(@PathVariable Long reviewId, @Valid @RequestBody ReviewSubmitRequest req) {
        reviewService.submitReview(reviewId, req);
        return Result.ok();
    }
}
