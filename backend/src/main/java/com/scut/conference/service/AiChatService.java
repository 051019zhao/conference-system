package com.scut.conference.service;

import com.scut.conference.common.BizException;
import com.scut.conference.entity.ConferenceInfo;
import com.scut.conference.entity.ScheduleItem;
import com.scut.conference.entity.Topic;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能会议助手:把数据库中的会议真实信息(会议资料/专题/日程/重要日期/投稿规则)
 * 注入提示词作为上下文,再由大模型回答用户问题——轻量级 RAG(检索增强生成)。
 * AI 只被允许依据资料回答,资料中没有的信息引导用户联系会务邮箱。
 */
@Service
public class AiChatService {

    private final AiService aiService;
    private final ContentService contentService;

    public AiChatService(AiService aiService, ContentService contentService) {
        this.aiService = aiService;
        this.contentService = contentService;
    }

    /**
     * @param question 用户问题
     * @param history  近几轮对话 [{role:'user'|'assistant', content}, ...](可为空)
     * @return 助手回复文本
     */
    public String reply(String question, List<AiService.Message> history) {
        if (question == null || question.isBlank()) {
            throw new BizException("请输入问题");
        }
        List<AiService.Message> messages = new ArrayList<>();
        messages.add(new AiService.Message("system", systemPromptWithFacts()));
        // 只带最近 6 轮,控制上下文长度(免费模型 token 有限)
        if (history != null) {
            int from = Math.max(0, history.size() - 6);
            messages.addAll(history.subList(from, history.size()));
        }
        messages.add(new AiService.Message("user", question));
        return aiService.chat(messages);
    }

    /** 组装"数据库事实"上下文:这是本功能与数据库课的结合点 */
    private String systemPromptWithFacts() {
        ConferenceInfo c = contentService.conferenceInfo();
        StringBuilder facts = new StringBuilder();
        if (c != null) {
            facts.append("会议名称:").append(c.getName()).append('\n');
            if (c.getVenue() != null) {
                facts.append("地点:").append(c.getVenue()).append('\n');
            }
            if (c.getStartDate() != null) {
                facts.append("会期:").append(c.getStartDate()).append(" 至 ").append(c.getEndDate()).append('\n');
            }
            if (c.getSubmitDeadline() != null) {
                facts.append("投稿截止:").append(c.getSubmitDeadline()).append('\n');
            }
            if (c.getNotifyDeadline() != null) {
                facts.append("录用通知:").append(c.getNotifyDeadline()).append('\n');
            }
            if (c.getFinalDeadline() != null) {
                facts.append("终稿截止:").append(c.getFinalDeadline()).append('\n');
            }
            if (c.getContactEmail() != null) {
                facts.append("会务邮箱:").append(c.getContactEmail()).append('\n');
            }
        }
        List<Topic> topics = contentService.listTopics();
        if (!topics.isEmpty()) {
            facts.append("征文专题:").append(topics.stream().map(Topic::getName).reduce((a, b) -> a + "、" + b).orElse("")).append('\n');
        }
        List<ScheduleItem> schedule = contentService.listSchedule();
        if (!schedule.isEmpty()) {
            facts.append("会议日程要点(最多前8条):");
            schedule.stream().limit(8).forEach(s -> facts.append("\n- ")
                    .append(s.getDay()).append(' ').append(s.getStartTime() == null ? "" : s.getStartTime() + " ")
                    .append(s.getTitle()).append(s.getLocation() == null ? "" : "(" + s.getLocation() + ")"));
            facts.append('\n');
        }
        facts.append("投稿流程:注册(邮箱验证码+管理员审核)→登录→在线投稿(PDF主稿≤20MB,可存草稿,可传zip附属资料)→初审→专家评审→终审→录用后上传终稿。\n");

        return "你是会议\"" + (c == null ? "" : c.getName()) + "\"的智能客服助手。请仅依据下面的会议资料回答问题;"
                + "资料中没有的信息就诚实说明不知道,并建议用户发邮件联系会务邮箱,不要编造。"
                + "回答使用简体中文,简洁明了,一般不超过 150 字。\n\n【会议资料(来自系统数据库)】\n" + facts;
    }
}
