package com.scut.conference.security;

import com.scut.conference.common.BizException;

/**
 * 当前登录用户上下文:拦截器解析 Token 后写入,Service 层随时读取,请求结束时清空
 */
public class UserContext {

    private static final ThreadLocal<Ctx> HOLDER = new ThreadLocal<>();

    private record Ctx(Long userId, String role) {}

    public static void set(Long userId, String role) {
        HOLDER.set(new Ctx(userId, role));
    }

    public static Long getUserId() {
        Ctx ctx = HOLDER.get();
        return ctx == null ? null : ctx.userId();
    }

    public static String getRole() {
        Ctx ctx = HOLDER.get();
        return ctx == null ? null : ctx.role();
    }

    /** 取当前用户 id,未登录直接抛业务异常(正常情况拦截器已保证登录) */
    public static Long requireUserId() {
        Long id = getUserId();
        if (id == null) {
            throw new BizException("未登录");
        }
        return id;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
