package com.scut.conference.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 手写 JWT 的单元测试:签发-解析往返、防篡改、防过期
 */
class JwtUtilTest {

    private JwtUtil newUtil(int expireHours) {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "secret", "unit-test-secret-key-0123456789");
        ReflectionTestUtils.setField(util, "expireHours", expireHours);
        return util;
    }

    @Test
    void tokenRoundTrip() {
        JwtUtil util = newUtil(1);
        String token = util.generate(42L, "AUTHOR");
        JwtUtil.TokenInfo info = util.parse(token);
        assertNotNull(info, "合法 Token 应解析成功");
        assertEquals(42L, info.userId());
        assertEquals("AUTHOR", info.role());
    }

    @Test
    void tamperedTokenRejected() {
        JwtUtil util = newUtil(1);
        String token = util.generate(1L, "ADMIN");
        // 篡改 payload 任意一位,签名校验应失败
        assertNull(util.parse(token.substring(0, token.length() - 2) + "xy"),
                "被篡改的 Token 必须拒绝");
    }

    @Test
    void garbageTokenRejected() {
        JwtUtil util = newUtil(1);
        assertNull(util.parse("not.a.jwt"));
        assertNull(util.parse(""));
    }

    @Test
    void expiredTokenRejected() {
        // 过期时间设为 0 小时,生成即过期
        JwtUtil util = newUtil(0);
        String token = util.generate(7L, "EXPERT");
        assertNull(util.parse(token), "过期 Token 必须拒绝");
    }
}
