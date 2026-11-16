package com.scut.conference.service;

import com.scut.conference.common.BizException;
import com.scut.conference.entity.Paper;
import com.scut.conference.entity.PaperAuthor;
import com.scut.conference.entity.Topic;
import com.scut.conference.mapper.AiAnalysisMapper;
import com.scut.conference.mapper.PaperAuthorMapper;
import com.scut.conference.mapper.PaperMapper;
import com.scut.conference.mapper.TopicMapper;
import com.scut.conference.security.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 论文预审:提交任务立即返回,后台线程调用大模型生成,结果落库(ai_analysis)。
 * 定位是"人机协同"——AI 报告仅供评审专家与管理员参考,不进入作者视角,
 * 最终录用决定仍由人工评审与管理员做出(学术伦理边界,答辩讲解点)。
 */
@Service
public class AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);
    private static final String BIZ_PRECHECK = "PAPER_PRECHECK";

    private final AiAnalysisMapper aiAnalysisMapper;
    private final PaperMapper paperMapper;
    private final PaperAuthorMapper paperAuthorMapper;
    private final TopicMapper topicMapper;
    private final com.scut.conference.mapper.ReviewMapper reviewMapper;
    private final AiService aiService;
    private final OperationLogService operationLogService;

    /** 单线程执行器:串行调用,避免并发打爆免费模型限流 */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ai-precheck");
        t.setDaemon(true);
        return t;
    });

    public AiAnalysisService(AiAnalysisMapper aiAnalysisMapper, PaperMapper paperMapper,
                             PaperAuthorMapper paperAuthorMapper, TopicMapper topicMapper,
                             com.scut.conference.mapper.ReviewMapper reviewMapper,
                             AiService aiService, OperationLogService operationLogService) {
        this.aiAnalysisMapper = aiAnalysisMapper;
        this.paperMapper = paperMapper;
        this.paperAuthorMapper = paperAuthorMapper;
        this.topicMapper = topicMapper;
        this.reviewMapper = reviewMapper;
        this.aiService = aiService;
        this.operationLogService = operationLogService;
    }

    /** 管理员触发 AI 预审:论文须处于外审中;立即返回任务 id,生成异步进行 */
    public Long startPrecheck(Long paperId) {
        Paper paper = paperMapper.findById(paperId);
        if (paper == null) {
            throw new BizException("论文不存在");
        }
        if (!"REVIEWING".equals(paper.getStatus())) {
            throw new BizException("仅外审中的论文可生成 AI 预审");
        }
        if (!aiService.isReady()) {
            throw new BizException("AI 服务未配置,无法生成预审");
        }
        if (aiAnalysisMapper.countRunning(BIZ_PRECHECK, paperId) > 0) {
            throw new BizException("该论文已有 AI 预审任务在生成中,请稍候刷新查看");
        }
        aiAnalysisMapper.insertTask(BIZ_PRECHECK, paperId, aiService.getModel(), UserContext.getUserId());
        Long taskId = ((Number) aiAnalysisMapper.findLatest(BIZ_PRECHECK, paperId).get("id")).longValue();
        executor.submit(() -> runPrecheck(taskId, paperId));
        operationLogService.log("AI_PRECHECK", "触发论文 #" + paperId + " 的 AI 预审");
        return taskId;
    }

    /** 最新一份预审(详情页轮询/查看用):未生成过返回 null */
    public Map<String, Object> latestPrecheck(Long paperId) {
        return aiAnalysisMapper.findLatest(BIZ_PRECHECK, paperId);
    }

    /**
     * 预审报告查询(带权限):仅管理员与被分配的评审专家可见,对作者保密——
     * AI 预审属内部参考,提前向作者披露可能影响评审公正。
     */
    public Map<String, Object> precheckForViewer(Long paperId) {
        Paper paper = paperMapper.findById(paperId);
        if (paper == null) {
            throw new BizException("论文不存在");
        }
        String role = UserContext.getRole();
        Long userId = UserContext.requireUserId();
        boolean allowed = "ADMIN".equals(role)
                || (Boolean.TRUE.equals(paper.getReviewPlanPublished())
                    && reviewMapper.findByPaperAndExpert(paperId, userId) != null);
        if (!allowed) {
            throw new BizException("AI 预审报告仅对评审专家与管理员开放");
        }
        return latestPrecheck(paperId);
    }

    private void runPrecheck(Long taskId, Long paperId) {
        try {
            aiAnalysisMapper.updateStatus(taskId, "RUNNING", null, null);
            Paper paper = paperMapper.findById(paperId);
            if (paper == null) {
                throw new BizException("论文不存在");
            }
            Topic topic = paper.getTopicId() == null ? null : topicMapper.findById(paper.getTopicId());
            List<PaperAuthor> authors = paperAuthorMapper.findByPaperId(paperId);

            String prompt = "论文标题:" + paper.getTitle() + "\n"
                    + "所属专题:" + (topic == null ? "未分类" : topic.getName()) + "\n"
                    + "关键词:" + paper.getKeywords() + "\n"
                    + "作者人数:" + authors.size() + "\n"
                    + "论文摘要:\n" + paper.getPaperAbstract() + "\n\n"
                    + "请作为学术会议评审助理,基于以上信息输出结构化纯文本预审报告(不要使用任何 markdown 符号),"
                    + "按以下小节组织:\n"
                    + "【内容总结】两三句话概括论文做了什么。\n"
                    + "【优点】列出 2-4 条。\n"
                    + "【不足与风险】列出 2-4 条,包括从摘要能看出的局限性。\n"
                    + "【五维评分建议】主题契合度/创新性/完整性/技术深度/表达质量,各给 1-5 分并附一句理由。\n"
                    + "【终审倾向参考】倾向录用 / 修改后再审 / 倾向退稿,三选一并说明理由。\n"
                    + "最后固定输出一行:本报告由 AI 自动生成,仅供参考,最终结论以人工评审为准。";

            String content = aiService.chat(List.of(
                    new AiService.Message("system",
                            "你是一个严谨的学术会议评审助理,善于从论文摘要出发做初步分析。"
                                    + "只输出纯文本,使用【】作为小节标记,不使用任何 markdown 语法。"),
                    new AiService.Message("user", prompt)));

            aiAnalysisMapper.updateStatus(taskId, "READY", content, null);
            log.info("AI 预审完成 paperId={} taskId={}", paperId, taskId);
        } catch (Exception e) {
            log.warn("AI 预审失败 taskId={}: {}", taskId, e.getMessage());
            String err = e.getMessage() == null ? "未知错误" : e.getMessage();
            aiAnalysisMapper.updateStatus(taskId, "FAILED", null,
                    err.length() > 500 ? err.substring(0, 500) : err);
        }
    }
}
