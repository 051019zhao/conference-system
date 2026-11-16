package com.scut.conference.service;

import com.scut.conference.common.BizException;
import com.scut.conference.common.DownloadFile;
import com.scut.conference.dto.PaperDtos.AuthorItem;
import com.scut.conference.dto.PaperDtos.PaperSubmitRequest;
import com.scut.conference.entity.ConferenceInfo;
import com.scut.conference.entity.Paper;
import com.scut.conference.entity.PaperAuthor;
import com.scut.conference.entity.PaperFile;
import com.scut.conference.entity.SysUser;
import com.scut.conference.entity.Topic;
import com.scut.conference.mapper.ConferenceMapper;
import com.scut.conference.mapper.PaperAuthorMapper;
import com.scut.conference.mapper.PaperFileMapper;
import com.scut.conference.mapper.PaperMapper;
import com.scut.conference.mapper.ReviewMapper;
import com.scut.conference.mapper.TopicMapper;
import com.scut.conference.mapper.UserMapper;
import com.scut.conference.security.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 论文:草稿/投稿 / 截止时间校验 / 我的投稿 / 详情(评审隔离) / 终稿 / 撤回 / 管理员状态流转
 * 状态机:DRAFT -> SUBMITTED -> REVIEWING -> ACCEPTED / REJECTED;ACCEPTED -> FINAL_SUBMITTED;
 *        SUBMITTED -> WITHDRAWN(作者撤回)
 * 并发安全:所有状态流转使用"WHERE 带原状态"的条件更新(CAS),影响行数为 0 即拒绝。
 */
@Service
public class PaperService {

    private static final Logger log = LoggerFactory.getLogger(PaperService.class);
    private static final int MAX_SUPPLEMENTS_PER_REQUEST = 3;

    private final PaperMapper paperMapper;
    private final PaperAuthorMapper paperAuthorMapper;
    private final PaperFileMapper paperFileMapper;
    private final TopicMapper topicMapper;
    private final ReviewMapper reviewMapper;
    private final ConferenceMapper conferenceMapper;
    private final FileStorageService fileStorage;
    private final OperationLogService operationLogService;
    private final UserMapper userMapper;
    private final MailService mailService;

    public PaperService(PaperMapper paperMapper, PaperAuthorMapper paperAuthorMapper,
                        PaperFileMapper paperFileMapper, TopicMapper topicMapper,
                        ReviewMapper reviewMapper, ConferenceMapper conferenceMapper,
                        FileStorageService fileStorage, OperationLogService operationLogService,
                        UserMapper userMapper, MailService mailService) {
        this.paperMapper = paperMapper;
        this.paperAuthorMapper = paperAuthorMapper;
        this.paperFileMapper = paperFileMapper;
        this.topicMapper = topicMapper;
        this.reviewMapper = reviewMapper;
        this.conferenceMapper = conferenceMapper;
        this.fileStorage = fileStorage;
        this.operationLogService = operationLogService;
        this.userMapper = userMapper;
        this.mailService = mailService;
    }

    // ---------------- 截止时间业务规则 ----------------

    /** 投稿/草稿编辑截止校验(会议信息中未配置截止日期则不限制) */
    private void checkSubmitDeadline() {
        ConferenceInfo info = conferenceMapper.find();
        if (info != null && info.getSubmitDeadline() != null
                && LocalDate.now().isAfter(info.getSubmitDeadline())) {
            throw new BizException("已过投稿截止时间(" + info.getSubmitDeadline() + "),无法进行该操作");
        }
    }

    /** 终稿提交截止校验 */
    private void checkFinalDeadline() {
        ConferenceInfo info = conferenceMapper.find();
        if (info != null && info.getFinalDeadline() != null
                && LocalDate.now().isAfter(info.getFinalDeadline())) {
            throw new BizException("已过终稿提交截止时间(" + info.getFinalDeadline() + ")");
        }
    }

    // ---------------- 作者:草稿与投稿 ----------------

    /**
     * 投稿入口:draft=true 存草稿(附件可选,可反复编辑);
     * draft=false 正式投稿(必须附件,且在投稿截止前)。
     */
    @Transactional
    public Long submit(PaperSubmitRequest req, MultipartFile file, List<MultipartFile> supplements, boolean draft) {
        checkSupplementCount(supplements);
        Topic topic = topicMapper.findById(req.topicId());
        if (topic == null) {
            throw new BizException("所属专题不存在");
        }
        if (!draft) {
            checkSubmitDeadline();
            validateFormalPaper(req.titleEn(), req.abstractEn(), req.keywords(), req.keywordsEn(),
                    req.authors().stream().anyMatch(a -> a.isCorresponding() && hasText(a.email())));
            if (file == null || file.isEmpty()) {
                throw new BizException("正式投稿必须上传论文主稿 PDF");
            }
        }
        Paper paper = new Paper();
        paper.setTitle(req.title());
        paper.setTitleEn(req.titleEn());
        paper.setPaperAbstract(req.abstractText());
        paper.setAbstractEn(req.abstractEn());
        paper.setKeywords(req.keywords());
        paper.setKeywordsEn(req.keywordsEn());
        paper.setTopicId(req.topicId());
        paper.setSubmitterId(UserContext.requireUserId());
        paper.setStatus(draft ? "DRAFT" : "SUBMITTED");
        paper.setFilePath(null);
        paperMapper.insert(paper);
        if (file != null && !file.isEmpty()) {
            String path = saveVersionedFile(paper.getId(), "MAIN", "papers", file);
            paperMapper.updateFilePath(paper.getId(), path);
        }
        saveSupplements(paper.getId(), supplements);
        saveAuthors(paper.getId(), req.authors());
        operationLogService.log(draft ? "DRAFT_SAVE" : "SUBMIT",
                (draft ? "保存草稿 #" : "投稿论文 #") + paper.getId() + " " + paper.getTitle());
        return paper.getId();
    }

    /** 草稿或待初审论文可在投稿截止前修改；已进入外审则锁定内容。 */
    @Transactional
    public void updateDraft(Long id, PaperSubmitRequest req, MultipartFile file, List<MultipartFile> supplements) {
        checkSupplementCount(supplements);
        Paper paper = mustFind(id);
        if (!paper.getSubmitterId().equals(UserContext.requireUserId())) {
            throw new BizException("只能编辑自己的论文");
        }
        if (!"DRAFT".equals(paper.getStatus()) && !"SUBMITTED".equals(paper.getStatus())) {
            throw new BizException("论文已进入评审流程，不能修改");
        }
        if (topicMapper.findById(req.topicId()) == null) {
            throw new BizException("所属专题不存在");
        }
        checkSubmitDeadline();
        if ("SUBMITTED".equals(paper.getStatus())) {
            validateFormalPaper(req.titleEn(), req.abstractEn(), req.keywords(), req.keywordsEn(),
                    req.authors().stream().anyMatch(a -> a.isCorresponding() && hasText(a.email())));
        }
        Paper meta = new Paper();
        meta.setId(id);
        meta.setTitle(req.title());
        meta.setTitleEn(req.titleEn());
        meta.setPaperAbstract(req.abstractText());
        meta.setAbstractEn(req.abstractEn());
        meta.setKeywords(req.keywords());
        meta.setKeywordsEn(req.keywordsEn());
        meta.setTopicId(req.topicId());
        requireCasSuccess(paperMapper.updateDraftMeta(meta));
        paperAuthorMapper.deleteByPaperId(id);
        saveAuthors(id, req.authors());
        if (file != null && !file.isEmpty()) {
            // 换主稿 = 新版本:旧版本保留在账本中供追溯
            String path = saveVersionedFile(id, "MAIN", "papers", file);
            paperMapper.updateFilePath(id, path);
        }
        saveSupplements(id, supplements);
        operationLogService.log("PAPER_UPDATE", "修改论文 #" + id + " " + req.title());
    }

    /** 草稿正式提交:DRAFT -> SUBMITTED(CAS) */
    public void submitDraft(Long id) {
        Paper paper = mustFind(id);
        if (!paper.getSubmitterId().equals(UserContext.requireUserId())) {
            throw new BizException("只能提交自己的草稿");
        }
        if (!"DRAFT".equals(paper.getStatus())) {
            throw new BizException("该论文不是草稿状态");
        }
        checkSubmitDeadline();
        if (paper.getFilePath() == null || paper.getFilePath().isBlank()) {
            throw new BizException("请先在草稿中上传论文附件再提交");
        }
        validateFormalPaper(paper.getTitleEn(), paper.getAbstractEn(), paper.getKeywords(), paper.getKeywordsEn(),
                paperAuthorMapper.findByPaperId(id).stream()
                        .anyMatch(a -> Boolean.TRUE.equals(a.getIsCorresponding()) && hasText(a.getEmail())));
        requireCasSuccess(paperMapper.casUpdateStatus(id, "DRAFT", "SUBMITTED"));
        operationLogService.log("SUBMIT", "草稿正式投稿 #" + id + " " + paper.getTitle());
    }

    /** 作者撤回投稿:仅待初审状态可撤回 */
    public void withdraw(Long id) {
        Paper paper = mustFind(id);
        if (!paper.getSubmitterId().equals(UserContext.requireUserId())) {
            throw new BizException("只能撤回自己的论文");
        }
        if (!"SUBMITTED".equals(paper.getStatus())) {
            throw new BizException("论文已进入评审流程,请联系管理员处理");
        }
        requireCasSuccess(paperMapper.casUpdateStatus(id, "SUBMITTED", "WITHDRAWN"));
        operationLogService.log("WITHDRAW", "撤回投稿 #" + id + " " + paper.getTitle());
    }

    /** 撤回的论文恢复成可编辑草稿，重新提交时仍需通过正式投稿校验。 */
    @Transactional
    public void reopenWithdrawn(Long id) {
        Paper paper = mustFind(id);
        if (!paper.getSubmitterId().equals(UserContext.requireUserId())) {
            throw new BizException("只能恢复自己的论文");
        }
        checkSubmitDeadline();
        requireCasSuccess(paperMapper.casUpdateStatus(id, "WITHDRAWN", "DRAFT"));
        operationLogService.log("PAPER_REOPEN", "恢复已撤回论文为草稿 #" + id);
    }

    /** 作者删除草稿或已撤回论文：仅隐藏记录，保留文件和评审审计，可在回收站恢复。 */
    @Transactional
    public void moveToTrash(Long id) {
        Paper paper = mustFind(id);
        Long authorId = UserContext.requireUserId();
        if (!paper.getSubmitterId().equals(authorId)
                || (!"DRAFT".equals(paper.getStatus()) && !"WITHDRAWN".equals(paper.getStatus()))) {
            throw new BizException("仅能删除自己的草稿或已撤回论文");
        }
        if (paperMapper.moveToTrash(id, authorId) != 1) {
            throw new BizException("删除冲突，请刷新后重试");
        }
        operationLogService.log("PAPER_TRASH", "论文移入回收站 #" + id);
    }

    @Transactional
    public void restoreFromTrash(Long id) {
        if (paperMapper.restoreFromTrash(id, UserContext.requireUserId()) != 1) {
            throw new BizException("回收站中没有这篇可恢复的论文");
        }
        operationLogService.log("PAPER_RESTORE", "从回收站恢复论文 #" + id);
    }

    public List<Map<String, Object>> myPapers() {
        return paperMapper.listBySubmitter(UserContext.requireUserId());
    }

    public List<Map<String, Object>> myTrash() {
        return paperMapper.listDeletedBySubmitter(UserContext.requireUserId());
    }

    /** 管理后台论文列表(按状态/关键词过滤,分页返回 list+total) */
    public Map<String, Object> adminPapers(String status, String keyword, int page, int size) {
        String st = status == null ? "" : status;
        String kw = keyword == null ? "" : keyword;
        Map<String, Object> data = new HashMap<>();
        data.put("list", paperMapper.adminList(st, kw, (page - 1) * size, size));
        data.put("total", paperMapper.countAdminList(st, kw));
        return data;
    }

    /** 前台"录用论文"列表(含已提交终稿的) */
    public List<Map<String, Object>> listAcceptedPublic() {
        return paperMapper.listAccepted();
    }

    /** 公开下载已录用论文:优先提供终稿,尚未提交终稿时提供投稿主稿。 */
    public DownloadFile downloadAcceptedPublic(Long id) {
        Paper paper = mustFind(id);
        if (!"ACCEPTED".equals(paper.getStatus()) && !"FINAL_SUBMITTED".equals(paper.getStatus())) {
            throw new BizException("该论文尚未录用,不可公开下载");
        }
        boolean hasFinal = paper.getFinalFilePath() != null && !paper.getFinalFilePath().isBlank();
        String path = hasFinal ? paper.getFinalFilePath() : paper.getFilePath();
        if (path == null || path.isBlank()) {
            throw new BizException("该论文暂无可下载文件");
        }
        return new DownloadFile(fileStorage.loadAsResource(path),
                paper.getTitle() + (hasFinal ? "-终稿.pdf" : "-主稿.pdf"));
    }

    // ---------------- 详情与评审信息隔离 ----------------

    /**
     * 论文详情的可见性规则:
     * - 论文本体:投稿人 / 被分配专家 / 管理员;
     * - 评审意见:管理员可见全部;评审专家只能看到自己的;投稿人仅在论文有终审结论后可见匿名意见。
     */
    public Map<String, Object> detail(Long id) {
        Paper paper = mustFind(id);
        String role = UserContext.getRole();
        Long userId = UserContext.requireUserId();
        boolean isAdmin = "ADMIN".equals(role);
        boolean isOwner = paper.getSubmitterId().equals(userId);
        boolean isAssignedExpert = Boolean.TRUE.equals(paper.getReviewPlanPublished())
                && reviewMapper.findByPaperAndExpert(id, userId) != null;
        if (!isAdmin && !isOwner && !isAssignedExpert) {
            throw new BizException("无权查看该论文");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("paper", paper);
        data.put("reviewReturned", reviewReturned(paper));
        data.put("authors", paperAuthorMapper.findByPaperId(id));
        data.put("files", paperFileMapper.findByPaperId(id));
        Topic topic = paper.getTopicId() == null ? null : topicMapper.findById(paper.getTopicId());
        data.put("topicName", topic == null ? null : topic.getName());

        boolean decided = "ACCEPTED".equals(paper.getStatus())
                || "REJECTED".equals(paper.getStatus())
                || "FINAL_SUBMITTED".equals(paper.getStatus());
        List<Map<String, Object>> reviews;
        if (isAdmin) {
            reviews = reviewMapper.findWithExpertByPaperId(id);
        } else if (isAssignedExpert) {
            // 专家视角:只返回自己的评审,他人意见不可见
            reviews = reviewMapper.findWithExpertByPaperId(id).stream()
                    .filter(r -> userId.equals(((Number) r.get("expertId")).longValue()))
                    .toList();
        } else if (isOwner && decided) {
            // 作者视角:终审后可见,但彻底匿名(剥离专家身份字段)
            reviews = reviewMapper.findWithExpertByPaperId(id).stream()
                    .peek(r -> {
                        r.remove("expertName");
                        r.remove("expertId");
                    })
                    .toList();
        } else {
            reviews = List.of();
        }
        data.put("reviews", reviews);
        return data;
    }

    /** 下载论文文件 type=original|final,权限同详情 */
    public DownloadFile downloadFile(Long id, String type) {
        Paper paper = mustFind(id);
        String role = UserContext.getRole();
        Long userId = UserContext.requireUserId();
        boolean allowed = "ADMIN".equals(role)
                || paper.getSubmitterId().equals(userId)
                || (Boolean.TRUE.equals(paper.getReviewPlanPublished())
                    && reviewMapper.findByPaperAndExpert(paper.getId(), userId) != null);
        if (!allowed) {
            throw new BizException("无权查看该论文");
        }
        boolean isFinal = "final".equals(type);
        String path = isFinal ? paper.getFinalFilePath() : paper.getFilePath();
        if (path == null || path.isBlank()) {
            throw new BizException("该论文还没有上传文件");
        }
        String ext = path.lastIndexOf('.') >= 0 ? path.substring(path.lastIndexOf('.')) : "";
        String name = paper.getTitle() + (isFinal ? "-终稿" : "") + ext;
        return new DownloadFile(fileStorage.loadAsResource(path), name);
    }

    /** 录用后作者上传终稿(终稿截止前) */
    @Transactional
    public void submitFinal(Long id, MultipartFile file) {
        Paper paper = mustFind(id);
        if (!paper.getSubmitterId().equals(UserContext.requireUserId())) {
            throw new BizException("只能对自己的论文上传终稿");
        }
        if (!"ACCEPTED".equals(paper.getStatus())) {
            throw new BizException("论文录用后才能上传终稿");
        }
        checkFinalDeadline();
        String path = saveVersionedFile(id, "FINAL", "finals", file);
        requireCasSuccess(paperMapper.updateFinalFile(id, path));
        operationLogService.log("FINAL", "论文 #" + id + " 提交终稿");
    }

    // ---------------- 附属资料与附件账本 ----------------

    /** 作者上传附属资料(zip/rar/7z 压缩包或 PDF/图片):草稿或待初审状态、投稿截止前 */
    public void addSupplement(Long paperId, MultipartFile file) {
        Paper paper = mustFind(paperId);
        if (!paper.getSubmitterId().equals(UserContext.requireUserId())) {
            throw new BizException("只能为自己的论文上传附属资料");
        }
        if (!"DRAFT".equals(paper.getStatus()) && !"SUBMITTED".equals(paper.getStatus())) {
            throw new BizException("仅草稿或待初审状态可管理附属资料");
        }
        checkSubmitDeadline();
        saveVersionedFile(paperId, "SUPPLEMENT", "supplements", file);
        operationLogService.log("SUPPLEMENT", "论文 #" + paperId + " 上传附属资料 " + file.getOriginalFilename());
    }

    /** 删除附属资料(仅作者本人、未进入外审) */
    public void deleteSupplement(Long fileId) {
        PaperFile pf = mustFindFile(fileId);
        Paper paper = mustFind(pf.getPaperId());
        if (!paper.getSubmitterId().equals(UserContext.requireUserId())) {
            throw new BizException("只能删除自己论文的附属资料");
        }
        if (!"SUPPLEMENT".equals(pf.getFileType())) {
            throw new BizException("仅附属资料可删除,主稿与终稿请通过重新上传更新版本");
        }
        if (!"DRAFT".equals(paper.getStatus()) && !"SUBMITTED".equals(paper.getStatus())) {
            throw new BizException("论文已进入评审流程,附属资料不可再修改");
        }
        paperFileMapper.deleteById(fileId);
        operationLogService.log("SUPPLEMENT_DELETE", "论文 #" + pf.getPaperId() + " 删除附属资料 " + pf.getFileName());
    }

    /** 按附件账本 ID 下载任意附件(权限同论文详情) */
    public DownloadFile downloadById(Long fileId) {
        PaperFile pf = mustFindFile(fileId);
        Paper paper = mustFind(pf.getPaperId());
        checkViewPermission(paper);
        return new DownloadFile(fileStorage.loadAsResource(pf.getStoredPath()), pf.getFileName());
    }

    /** 论文可见权限:投稿人 / 被分配专家 / 管理员 */
    private void checkViewPermission(Paper paper) {
        String role = UserContext.getRole();
        Long userId = UserContext.requireUserId();
        boolean allowed = "ADMIN".equals(role)
                || paper.getSubmitterId().equals(userId)
                || (Boolean.TRUE.equals(paper.getReviewPlanPublished())
                    && reviewMapper.findByPaperAndExpert(paper.getId(), userId) != null);
        if (!allowed) {
            throw new BizException("无权查看该论文");
        }
    }

    // ---------------- 管理员状态流转(全部 CAS) ----------------

    /** 初审通过:进入外审 */
    public void firstPass(Long id) {
        Paper paper = mustFind(id);
        if (!"SUBMITTED".equals(paper.getStatus())) {
            throw new BizException("只有待初审的论文才能执行初审操作");
        }
        requireCasSuccess(paperMapper.casUpdateStatus(id, "SUBMITTED", "REVIEWING"));
        operationLogService.log("FIRST_PASS", "初审通过论文 #" + id + " " + paper.getTitle());
    }

    /** 初审退稿；外审后的退稿由终审统一处理。 */
    public void reject(Long id) {
        Paper paper = mustFind(id);
        if ("SUBMITTED".equals(paper.getStatus())) {
            requireCasSuccess(paperMapper.casUpdateStatus(id, "SUBMITTED", "REJECTED"));
        } else {
            throw new BizException("仅待初审论文可直接退稿，外审论文请在意见返回后终审");
        }
        operationLogService.log("REJECT", "退稿论文 #" + id + " " + paper.getTitle());
    }

    /** 终审：评审方案已发布，所有受邀专家的意见均已返回。 */
    @Transactional
    public void decide(Long id, String result) {
        if (!"ACCEPTED".equals(result) && !"REJECTED".equals(result)) {
            throw new BizException("终审结果不合法");
        }
        Paper paper = mustFind(id);
        if (!"REVIEWING".equals(paper.getStatus())) {
            throw new BizException("只有外审中的论文才能终审");
        }
        if (!reviewReturned(paper)) {
            throw new BizException("评审意见尚未全部返回，不能终审");
        }
        requireCasSuccess(paperMapper.casUpdateStatus(id, "REVIEWING", result));
        operationLogService.log("DECIDE", "终审论文 #" + id + " 结果:" + ("ACCEPTED".equals(result) ? "录用" : "退稿"));
        if ("ACCEPTED".equals(result)) {
            SysUser author = userMapper.findById(paper.getSubmitterId());
            if (author != null && author.getEmail() != null && !author.getEmail().isBlank()) {
                String email = author.getEmail();
                String subject = "论文录用通知：《" + paper.getTitle() + "》";
                String body = "您好，您投稿的论文《" + paper.getTitle()
                        + "》已被会议录用。请登录会议系统查看评审意见，并按会议要求提交终稿。";
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            mailService.send(email, subject, body);
                        } catch (Exception ex) {
                            log.warn("论文 #{} 已录用，但通知邮件发送失败: {}", id, ex.getMessage());
                        }
                    }
                });
            }
        }
    }

    private boolean reviewReturned(Paper paper) {
        if (!"REVIEWING".equals(paper.getStatus())
                || !Boolean.TRUE.equals(paper.getReviewPlanPublished())) return false;
        int assigned = reviewMapper.countByPaperId(paper.getId());
        return assigned > 0 && reviewMapper.countSubmittedByPaperId(paper.getId()) == assigned;
    }

    // ---------------- 辅助 ----------------

    /**
     * 存文件并写入版本账本:主稿和终稿的旧版本置为非当前,附属资料可多份同时有效。
     * paper 表的 file_path/final_file_path 仅作为"当前主稿"的冗余指针加速查询。
     */
    private String saveVersionedFile(Long paperId, String fileType, String subdir, MultipartFile file) {
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.lastIndexOf('.') >= 0) {
            ext = original.substring(original.lastIndexOf('.')).toLowerCase();
        }
        String path = "SUPPLEMENT".equals(fileType)
                ? fileStorage.storeSupplement(file)
                : fileStorage.store(file, subdir, true);
        if (!"SUPPLEMENT".equals(fileType)) {
            paperFileMapper.markOldVersionsNotCurrent(paperId, fileType);
        }
        PaperFile pf = new PaperFile();
        pf.setPaperId(paperId);
        pf.setFileType(fileType);
        pf.setFileName(original == null ? "unnamed" + ext : original);
        pf.setStoredPath(path);
        pf.setFileExt(ext.replace(".", ""));
        pf.setFileSize(file.getSize());
        pf.setVersionNo(paperFileMapper.maxVersion(paperId, fileType) + 1);
        pf.setIsCurrent(true);
        pf.setUploadedBy(UserContext.requireUserId());
        paperFileMapper.insert(pf);
        return path;
    }

    private void checkSupplementCount(List<MultipartFile> supplements) {
        if (supplements != null && supplements.size() > MAX_SUPPLEMENTS_PER_REQUEST) {
            throw new BizException("每次最多上传 3 份证明材料");
        }
    }

    private void saveSupplements(Long paperId, List<MultipartFile> supplements) {
        if (supplements == null) return;
        for (MultipartFile supplement : supplements) {
            saveVersionedFile(paperId, "SUPPLEMENT", "supplements", supplement);
        }
    }

    private PaperFile mustFindFile(Long fileId) {
        PaperFile pf = paperFileMapper.findById(fileId);
        if (pf == null) {
            throw new BizException("附件不存在");
        }
        return pf;
    }

    private void saveAuthors(Long paperId, List<AuthorItem> items) {
        List<PaperAuthor> authors = new ArrayList<>();
        int order = 1;
        for (AuthorItem item : items) {
            PaperAuthor author = new PaperAuthor();
            author.setPaperId(paperId);
            author.setName(item.name());
            author.setPinyin(item.pinyin());
            author.setEmail(item.email());
            author.setAffiliation(item.affiliation());
            author.setPostalCode(item.postalCode());
            author.setSortNo(order++);
            author.setIsCorresponding(item.isCorresponding());
            authors.add(author);
        }
        paperAuthorMapper.insertBatch(authors);
    }

    private void validateFormalPaper(String titleEn, String abstractEn, String keywords,
                                     String keywordsEn, boolean hasContactAuthor) {
        if (!hasText(titleEn) || !hasText(abstractEn)) {
            throw new BizException("正式投稿需填写英文标题和英文摘要");
        }
        if (keywordCount(keywords) < 3 || keywordCount(keywords) > 5
                || keywordCount(keywordsEn) < 3 || keywordCount(keywordsEn) > 5) {
            throw new BizException("中英文关键词均需填写 3 至 5 个，以逗号分隔");
        }
        if (!hasContactAuthor) {
            throw new BizException("请至少指定一位填写邮箱的通讯作者");
        }
    }

    private int keywordCount(String value) {
        if (!hasText(value)) return 0;
        return (int) java.util.Arrays.stream(value.split("[,，]"))
                .map(String::trim).filter(s -> !s.isEmpty()).count();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Paper mustFind(Long id) {
        Paper paper = paperMapper.findById(id);
        if (paper == null) {
            throw new BizException("论文不存在");
        }
        return paper;
    }

    /** CAS 更新影响行数为 0 说明状态已被并发请求改变,拒绝本次操作 */
    private void requireCasSuccess(int rows) {
        if (rows == 0) {
            throw new BizException("操作冲突:论文状态刚被其他操作修改,请刷新后重试");
        }
    }
}
