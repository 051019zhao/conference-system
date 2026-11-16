package com.scut.conference.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scut.conference.common.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * 大语言模型调用服务(OpenAI 兼容协议,默认智谱 GLM-4-Flash 免费模型)。
 * 手写 java.net.http 调用,零第三方依赖,答辩可逐行讲解。
 * 未配置 API Key 时优雅降级:抛业务异常提示"AI 服务未配置",不影响其他功能。
 */
@Service
public class AiService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${app.ai.enabled}")
    private boolean enabled;

    @Value("${app.ai.api-key:}")
    private String apiKey;

    @Value("${app.ai.base-url}")
    private String baseUrl;

    @Value("${app.ai.model}")
    private String model;

    @Value("${app.ai.timeout-seconds}")
    private int timeoutSeconds;

    public boolean isReady() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    public String getModel() {
        return model;
    }

    /**
     * 通用对话补全。messages 为 [ {role, content}, ... ] 的有序列表。
     * 返回模型输出的纯文本内容。
     */
    public String chat(List<Message> messages) {
        if (!isReady()) {
            throw new BizException("AI 服务未配置:请在环境变量 AI_API_KEY 中配置大模型 API Key(智谱 open.bigmodel.cn 可免费申请)");
        }
        try {
            ObjectNode body = MAPPER.createObjectNode();
            body.put("model", model);
            body.put("temperature", 0.3);
            body.put("max_tokens", 1500);
            ArrayNode arr = body.putArray("messages");
            for (Message m : messages) {
                ObjectNode node = arr.addObject();
                node.put("role", m.role());
                node.put("content", m.content());
            }
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
            // AI is optional: do not open network resources while starting the whole application.
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new BizException("AI 服务返回错误(HTTP " + response.statusCode() + "):"
                        + response.body().replaceAll("\\s+", " ").substring(0, Math.min(200, response.body().length())));
            }
            JsonNode root = MAPPER.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new BizException("AI 服务返回内容为空");
            }
            return content.asText();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("调用 AI 服务失败:" + e.getMessage());
        }
    }

    /** 对话消息(role: system / user / assistant) */
    public record Message(String role, String content) {}
}
