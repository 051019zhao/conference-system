package com.scut.conference.controller;

import com.scut.conference.common.Result;
import com.scut.conference.dto.PaperDtos.PaperSubmitRequest;
import com.scut.conference.security.RequireRole;
import com.scut.conference.service.PaperService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/** 作者投稿接口(详情/下载对被分配专家和管理员开放) */
@RestController
@RequestMapping("/api/paper")
public class PaperController {

    private final PaperService paperService;

    public PaperController(PaperService paperService) {
        this.paperService = paperService;
    }

    /** multipart 投稿:file=附件(≤20MB,存草稿时可不传),paper=论文信息 JSON;draft=true 存草稿 */
    @RequireRole("AUTHOR")
    @PostMapping(value = "/submit", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Long> submit(@RequestPart(value = "file", required = false) MultipartFile file,
                               @RequestPart(value = "supplements", required = false) List<MultipartFile> supplements,
                               @RequestPart("paper") @Valid PaperSubmitRequest paper,
                               @RequestParam(defaultValue = "false") boolean draft) {
        return Result.ok(paperService.submit(paper, file, supplements, draft));
    }

    /** 编辑草稿或待初审论文(投稿截止前;附件可选替换) */
    @RequireRole("AUTHOR")
    @PostMapping(value = "/draft/{id}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Void> updateDraft(@PathVariable Long id,
                                    @RequestPart(value = "file", required = false) MultipartFile file,
                                    @RequestPart(value = "supplements", required = false) List<MultipartFile> supplements,
                                    @RequestPart("paper") @Valid PaperSubmitRequest paper) {
        paperService.updateDraft(id, paper, file, supplements);
        return Result.ok();
    }

    /** 草稿正式提交(DRAFT -> SUBMITTED) */
    @RequireRole("AUTHOR")
    @PostMapping("/draft/{id}/submit")
    public Result<Void> submitDraft(@PathVariable Long id) {
        paperService.submitDraft(id);
        return Result.ok();
    }

    @RequireRole("AUTHOR")
    @GetMapping("/my")
    public Result<?> my() {
        return Result.ok(paperService.myPapers());
    }

    @RequireRole("AUTHOR")
    @GetMapping("/my/trash")
    public Result<?> myTrash() {
        return Result.ok(paperService.myTrash());
    }

    @RequireRole("AUTHOR")
    @PostMapping("/{id}/reopen")
    public Result<Void> reopen(@PathVariable Long id) {
        paperService.reopenWithdrawn(id);
        return Result.ok();
    }

    @RequireRole("AUTHOR")
    @DeleteMapping("/{id}")
    public Result<Void> moveToTrash(@PathVariable Long id) {
        paperService.moveToTrash(id);
        return Result.ok();
    }

    @RequireRole("AUTHOR")
    @PostMapping("/{id}/restore")
    public Result<Void> restore(@PathVariable Long id) {
        paperService.restoreFromTrash(id);
        return Result.ok();
    }

    /** 论文详情(权限在 Service 内校验:投稿人/被分配专家/管理员) */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.ok(paperService.detail(id));
    }

    /** 录用后上传终稿 */
    @RequireRole("AUTHOR")
    @PostMapping(value = "/{id}/final", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Void> submitFinal(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        paperService.submitFinal(id, file);
        return Result.ok();
    }

    /** 作者撤回投稿(仅待初审状态) */
    @RequireRole("AUTHOR")
    @PostMapping("/{id}/withdraw")
    public Result<Void> withdraw(@PathVariable Long id) {
        paperService.withdraw(id);
        return Result.ok();
    }

    /** 上传附属资料(zip/rar/7z 压缩包或 PDF/图片,≤20MB) */
    @RequireRole("AUTHOR")
    @PostMapping(value = "/{id}/supplement", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Void> addSupplement(@PathVariable Long id,
                                      @RequestPart("file") MultipartFile file) {
        paperService.addSupplement(id, file);
        return Result.ok();
    }

    /** 删除附属资料 */
    @RequireRole("AUTHOR")
    @org.springframework.web.bind.annotation.DeleteMapping("/file/{fileId}")
    public Result<Void> deleteSupplement(@PathVariable Long fileId) {
        paperService.deleteSupplement(fileId);
        return Result.ok();
    }

    /** 按附件 ID 下载(主稿任意版本/终稿/附属资料;权限在 Service 内校验) */
    @GetMapping("/file/{fileId}/download")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> downloadById(
            @PathVariable Long fileId) {
        return PublicController.downloadResponse(paperService.downloadById(fileId));
    }

    /** 下载论文附件 type=original|final */
    @GetMapping("/{id}/file")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> download(
            @PathVariable Long id, @RequestParam(defaultValue = "original") String type) {
        return PublicController.downloadResponse(paperService.downloadFile(id, type));
    }
}
