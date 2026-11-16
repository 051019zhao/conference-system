package com.scut.conference.controller;

import com.scut.conference.common.Result;
import com.scut.conference.security.RequireRole;
import com.scut.conference.service.AiAnalysisService;
import com.scut.conference.service.AiChatService;
import com.scut.conference.service.AiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI 增强功能接口:
 * - 前台智能会议助手(匿名,基于数据库事实的轻量 RAG)
 * - 管理员触发的论文 AI 预审(异步生成,报告仅评审专家/管理员可见)
 */
@RestController
@RequestMapping("/api")
public class AiController {

    private final AiChatService aiChatService;
    private final AiAnalysisService aiAnalysisService;
    private final AiService aiService;

    public AiController(AiChatService aiChatService, AiAnalysisService aiAnalysisService,
                        AiService aiService) {
        this.aiChatService = aiChatService;
        this.aiAnalysisService = aiAnalysisService;
        this.aiService = aiService;
    }

    /** 前台聊天(在 WebConfig 中随 /api/public/** 匿名放行);body: {message, history:[{role,content}]} */
    @PostMapping("/public/ai/chat")
    public Result<?> chat(@RequestBody ChatRequest req) {
        List<AiService.Message> history = new ArrayList<>();
        if (req.history() != null) {
            req.history().forEach(h -> history.add(new AiService.Message(h.role(), h.content())));
        }
        String reply = aiChatService.reply(req.message(), history);
        return Result.ok(Map.of("reply", reply, "model", aiService.getModel()));
    }

    /** 管理员触发论文 AI 预审(异步生成,稍后在论文详情查看) */
    @RequireRole("ADMIN")
    @PostMapping("/admin/paper/{id}/ai-precheck")
    public Result<?> startPrecheck(@PathVariable Long id) {
        aiAnalysisService.startPrecheck(id);
        return Result.ok(Map.of("started", true, "hint", "已开始生成,稍后在论文详情查看"));
    }

    /** 预审报告查询(权限:管理员/被分配专家;作者不可见) */
    @GetMapping("/paper/{id}/ai-precheck")
    public Result<?> precheck(@PathVariable Long id) {
        return Result.ok(aiAnalysisService.precheckForViewer(id));
    }

    /** 请求对象(history 为可选的多轮上下文) */
    public record ChatHistoryItem(String role, String content) {}

    public record ChatRequest(String message, List<ChatHistoryItem> history) {}
}
