package com.scut.conference.security;

import com.scut.conference.entity.SysUser;
import com.scut.conference.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * 认证与角色拦截器:
 * 1. 登录/注册与前台公开接口在 WebConfig 中放行,其余 /api/** 必须携带有效 Token;
 * 2. Token 有效则把 userId/role 写入 UserContext;
 * 3. 每次请求实时查库校验账号状态:Token 有效期内账号被禁用/删除,旧 Token 立即失效(不信任 Token 中的静态角色);
 * 4. 方法或类上标注 @RequireRole 时校验角色,不匹配返回 403。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    public AuthInterceptor(JwtUtil jwtUtil, UserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 跨域预检请求直接放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        JwtUtil.TokenInfo info = (token == null || token.isBlank()) ? null : jwtUtil.parse(token);
        if (info == null) {
            writeJson(response, 401, "未登录或登录已过期");
            return false;
        }

        // 实时校验账号状态:解决"Token 还没过期但账号已被禁用/待审核"的窗口问题
        SysUser user = userMapper.findById(info.userId());
        if (user == null) {
            writeJson(response, 401, "账号不存在");
            return false;
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            writeJson(response, 403,
                    user.getStatus() != null && user.getStatus() == 2 ? "账号待管理员审核" : "账号已被禁用");
            return false;
        }
        // 以数据库中的实时角色为准(防止角色变更后旧 Token 越权)
        UserContext.set(user.getId(), user.getRole());

        // 角色校验:方法上的注解优先,其次类上的注解
        RequireRole anno = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (anno == null) {
            anno = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (anno != null && Arrays.stream(anno.value()).noneMatch(r -> r.equals(user.getRole()))) {
            writeJson(response, 403, "无权限访问该功能");
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private void writeJson(HttpServletResponse response, int code, String msg) throws Exception {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + code + ",\"msg\":\"" + msg + "\",\"data\":null}");
    }
}
