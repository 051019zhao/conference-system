package com.scut.conference.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 手写 JWT(不引入第三方库):HMAC-SHA256 签名,结构 header.payload.signature
 * payload 中携带 uid / role / exp(过期时间戳,秒)
 */
@Component
public class JwtUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${app.jwt-secret}")
    private String secret;

    @Value("${app.jwt-expire-hours}")
    private int expireHours;

    /** Token 中解析出的用户信息 */
    public record TokenInfo(Long userId, String role) {}

    public String generate(Long userId, String role) {
        try {
            String header = b64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
            ObjectNode payload = MAPPER.createObjectNode();
            payload.put("uid", userId);
            payload.put("role", role);
            payload.put("exp", System.currentTimeMillis() / 1000 + expireHours * 3600L);
            String payloadStr = b64Url(payload.toString().getBytes(StandardCharsets.UTF_8));
            return header + "." + payloadStr + "." + sign(header + "." + payloadStr);
        } catch (Exception e) {
            throw new IllegalStateException("生成 Token 失败", e);
        }
    }

    /** 校验签名与有效期,非法或过期返回 null */
    public TokenInfo parse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            if (!sign(parts[0] + "." + parts[1]).equals(parts[2])) {
                return null;
            }
            JsonNode node = MAPPER.readTree(new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8));
            // RFC 7519:当前时间必须早于 exp,等于即视为过期
            if (node.path("exp").asLong() <= System.currentTimeMillis() / 1000) {
                return null;
            }
            return new TokenInfo(node.path("uid").asLong(), node.path("role").asText());
        } catch (Exception e) {
            return null;
        }
    }

    private String sign(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return b64Url(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    private static String b64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
