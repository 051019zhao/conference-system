package com.scut.conference.service;

import com.scut.conference.common.BizException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scut.conference.dto.ReviewDtos.ReviewSubmitRequest;
import com.scut.conference.entity.Paper;
import com.scut.conference.entity.Review;
import com.scut.conference.entity.SysUser;
import com.scut.conference.mapper.ExpertProfileMapper;
import com.scut.conference.mapper.PaperMapper;
import com.scut.conference.mapper.ReviewMapper;
import com.scut.conference.mapper.UserMapper;
import com.scut.conference.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
 * 评审:专家任务列表 / 提交评审 / 管理员分配(手动 + 按关键词自动匹配)
 */
@Service
public class ReviewService {

    /** 自动分配时最多选择的专家数 */
    private static final int AUTO_ASSIGN_LIMIT = 2;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Map<String, Set<String>> RUBRIC_OPTIONS = Map.of(
            "topicConformity", Set.of("VERY_WELL", "FAIRLY", "POORLY"),
            "topicCoverage", Set.of("BALANCED", "MISSING", "OVERSTRESSED"),
            "innovation", Set.of("HIGH", "SUFFICIENT", "SLIGHT", "NONE"),
            "integrity", Set.of("POOR", "FAIR", "GOOD", "OUTSTANDING"),
            "literary", Set.of("TOTAL", "MOSTLY", "PARTIAL", "INACCESSIBLE"),
            "technicalDepth", Set.of("SUPERFICIAL", "NON_SPECIALIST", "FIELD", "EXPERT"),
            "presentation", Set.of("SATISFACTORY", "IMPROVE", "POOR"),
            "organization", Set.of("SATISFACTORY", "IMPROVE", "POOR"));
    private static final Set<String> ACCEPT_GRADES = Set.of("STRONG_ACCEPT", "ACCEPT", "WEAK_ACCEPT");
    private static final Set<String> REJECT_GRADES = Set.of("STRONG_REJECT", "REJECT", "WEAK_REJECT");
    private static final Set<String> REJECT_REASONS = Set.of("QUALITY", "REWRITE", "TOPIC", "OTHER");

    private final ReviewMapper reviewMapper;
    private final PaperMapper paperMapper;
    private final UserMapper userMapper;
    private final ExpertProfileMapper expertProfileMapper;
    private final OperationLogService operationLogService;

    public ReviewService(ReviewMapper reviewMapper, PaperMapper paperMapper,
                         UserMapper userMapper, ExpertProfileMapper expertProfileMapper,
                         OperationLogService operationLogService) {
        this.reviewMapper = reviewMapper;
        this.paperMapper = paperMapper;
        this.userMapper = userMapper;
        this.expertProfileMapper = expertProfileMapper;
        this.operationLogService = operationLogService;
    }

    public List<Map<String, Object>> myTasks() {
        return reviewMapper.findTasksByExpertId(UserContext.requireUserId());
    }

    /** 专家提交评审表单(含五维评分;SQL 层限定 PENDING,防重复/并发提交) */
    @Transactional
    public void submitReview(Long reviewId, ReviewSubmitRequest req) {
        Review review = reviewMapper.findById(reviewId);
        if (review == null) {
            throw new BizException("评审任务不存在");
        }
        if (!review.getExpertId().equals(UserContext.requireUserId())) {
            throw new BizException("无权提交该评审");
        }
        if (!"PENDING".equals(review.getStatus())) {
            throw new BizException("该评审已提交,不能重复提交");
        }
        if (!"ACCEPT".equals(req.result()) && !"REJECT".equals(req.result())) {
            throw new BizException("评审结论不合法");
        }
        if (req.rubric() == null) {
            throw new BizException("请完整填写评审量表");
        }
        for (var entry : RUBRIC_OPTIONS.entrySet()) {
            String selected = req.rubric().get(entry.getKey());
            if (selected == null || !entry.getValue().contains(selected)) {
                throw new BizException("请完整填写评审量表：" + entry.getKey());
            }
        }
        if (req.recommendationGrade() == null
                || ("ACCEPT".equals(req.result()) && !ACCEPT_GRADES.contains(req.recommendationGrade()))
                || ("REJECT".equals(req.result()) && !REJECT_GRADES.contains(req.recommendationGrade()))) {
            throw new BizException("录用或退稿等级与评审结论不一致");
        }
        if ("REJECT".equals(req.result()) && (req.rejectionReason() == null
                || !REJECT_REASONS.contains(req.rejectionReason()))) {
            throw new BizException("请选择退稿原因");
        }
        Paper paper = paperMapper.findById(review.getPaperId());
        if (paper == null || !"REVIEWING".equals(paper.getStatus())
                || !Boolean.TRUE.equals(paper.getReviewPlanPublished())) {
            throw new BizException("论文当前不在外审阶段,评审已失效");
        }
        String rubricJson;
        try {
            rubricJson = JSON.writeValueAsString(req.rubric());
        } catch (JsonProcessingException e) {
            throw new BizException("评审量表格式不正确");
        }
        int rows = reviewMapper.submit(reviewId, req.overallComment(), req.revisionComment(),
                req.result(), score(req.rubric().get("topicConformity")),
                score(req.rubric().get("innovation")), score(req.rubric().get("integrity")),
                score(req.rubric().get("technicalDepth")), score(req.rubric().get("presentation")),
                req.recommendJournal(), rubricJson, req.recommendationGrade(),
                "REJECT".equals(req.result()) ? req.rejectionReason() : null);
        if (rows == 0) {
            throw new BizException("提交冲突:该评审已被提交,请刷新");
        }
        operationLogService.log("REVIEW", "提交评审:论文 #" + review.getPaperId() + " 结论:" + req.result());
    }

    private int score(String option) {
        return switch (option) {
            case "VERY_WELL", "HIGH", "OUTSTANDING", "EXPERT", "SATISFACTORY" -> 5;
            case "SUFFICIENT", "GOOD", "FIELD" -> 4;
            case "FAIRLY", "FAIR", "NON_SPECIALIST", "IMPROVE" -> 3;
            case "SLIGHT" -> 2;
            default -> 1;
        };
    }

    /**
     * 管理员手动分配:删除旧待评审记录后重建,已提交的评审保留。
     * 分配规则:仅 EXPERT 角色、账号必须启用(status=1)、排除论文投稿人(回避)、按用户去重。
     */
    @Transactional
    public void assign(Long paperId, List<Long> expertIds) {
        Paper paper = paperMapper.findByIdForUpdate(paperId);
        if (paper == null) {
            throw new BizException("论文不存在");
        }
        if (!"REVIEWING".equals(paper.getStatus())) {
            throw new BizException("请先初审通过,再分配评审专家");
        }
        if (Boolean.TRUE.equals(paper.getReviewPlanPublished())) {
            throw new BizException("评审方案已发布，不能再调整专家");
        }
        List<Long> distinctIds = new ArrayList<>(new LinkedHashSet<>(expertIds));
        for (Long expertId : distinctIds) {
            if (expertId.equals(paper.getSubmitterId())) {
                throw new BizException("不能把论文分配给它的投稿人(回避原则)");
            }
            SysUser user = userMapper.findById(expertId);
            if (user == null || !"EXPERT".equals(user.getRole())) {
                throw new BizException("所选用户不是评审专家");
            }
            if (user.getStatus() == null || user.getStatus() != 1) {
                throw new BizException("专家「" + user.getRealName() + "」账号当前不可用(禁用或待审核)");
            }
            var profile = expertProfileMapper.findByUserId(expertId);
            if (profile == null || Boolean.FALSE.equals(profile.getReviewEnabled())) {
                throw new BizException("专家「" + user.getRealName() + "」未开放评审");
            }
            if (!reviewMapper.supportsTopic(expertId, paperId)) {
                throw new BizException("专家「" + user.getRealName() + "」未选择该论文所属专题");
            }
            if (profile.getReviewCapacity() != null && profile.getReviewCapacity() > 0
                    && reviewMapper.countPendingByExpertExcludingPaper(expertId, paperId)
                    >= profile.getReviewCapacity()) {
                throw new BizException("专家「" + user.getRealName() + "」待评审任务已达上限");
            }
        }
        reviewMapper.deletePendingByPaperId(paperId);
        // 已提交过评审的专家跳过,避免违反 UNIQUE(paper_id, expert_id)
        List<Long> toInsert = distinctIds.stream()
                .filter(id -> reviewMapper.findByPaperAndExpert(paperId, id) == null)
                .toList();
        if (!toInsert.isEmpty()) {
            reviewMapper.insertBatch(paperId, toInsert);
        }
        operationLogService.log("ASSIGN", "论文 #" + paperId + " 手动分配 " + toInsert.size() + " 位评审专家");
    }

    /**
     * 自动分配:调用数据库函数 fn_match_experts(关键词匹配/回避/去重全部在库内完成),
     * 取匹配度前 2 位写入评审。返回被分配专家供前端提示。
     */
    @Transactional
    public List<Map<String, Object>> autoAssign(Long paperId) {
        Paper paper = paperMapper.findByIdForUpdate(paperId);
        if (paper == null) {
            throw new BizException("论文不存在");
        }
        if (!"REVIEWING".equals(paper.getStatus())) {
            throw new BizException("请先初审通过,再分配评审专家");
        }
        if (Boolean.TRUE.equals(paper.getReviewPlanPublished())) {
            throw new BizException("评审方案已发布，不能重新自动分配");
        }
        if (paper.getKeywords() == null || paper.getKeywords().isBlank()) {
            throw new BizException("论文未填写关键词,无法自动匹配");
        }
        List<Map<String, Object>> selected = reviewMapper.matchExperts(paperId, AUTO_ASSIGN_LIMIT);
        if (selected.isEmpty()) {
            throw new BizException("没有研究方向匹配的专家,请手动分配");
        }
        List<Long> ids = selected.stream()
                .map(e -> ((Number) e.get("userId")).longValue())
                .toList();
        reviewMapper.deletePendingByPaperId(paperId);
        reviewMapper.insertBatch(paperId, ids);
        String names = selected.stream().map(e -> String.valueOf(e.get("realName")))
                .reduce((a, b) -> a + "、" + b).orElse("");
        operationLogService.log("ASSIGN", "论文 #" + paperId + " 自动分配评审专家:" + names);
        return selected;
    }

    /** 批量发布评审方案；不足两位专家的论文保留草案并返回给管理员调整。 */
    @Transactional
    public Map<String, Object> publishPlans(List<Long> paperIds) {
        if (paperIds == null || paperIds.isEmpty()) {
            throw new BizException("请选择要发布评审方案的论文");
        }
        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(paperIds));
        if (ids.size() > 100) throw new BizException("每次最多发布 100 篇论文的评审方案");
        if (ids.contains(null)) throw new BizException("论文编号不能为空");
        ids.sort(Long::compareTo); // 并发批量发布按固定顺序加锁，避免互相等待。
        List<Long> insufficient = new ArrayList<>();
        List<Long> published = new ArrayList<>();
        for (Long id : ids) {
            Paper paper = paperMapper.findByIdForUpdate(id);
            if (paper == null || !"REVIEWING".equals(paper.getStatus())) {
                throw new BizException("论文 #" + id + " 不在送审阶段");
            }
            if (Boolean.TRUE.equals(paper.getReviewPlanPublished())) {
                throw new BizException("论文 #" + id + " 的评审方案已发布");
            }
            if (reviewMapper.countByPaperId(id) < AUTO_ASSIGN_LIMIT) {
                insufficient.add(id);
                continue;
            }
            published.add(id);
        }
        for (Long id : published) {
            if (paperMapper.publishReviewPlan(id) != 1) {
                throw new BizException("论文 #" + id + " 发布冲突，请刷新后重试");
            }
        }
        if (!published.isEmpty()) {
            operationLogService.log("REVIEW_PLAN_PUBLISH", "发布评审方案：" + published);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("publishedIds", published);
        result.put("insufficientIds", insufficient);
        return result;
    }
}
