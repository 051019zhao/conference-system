package com.scut.conference.service;

import com.scut.conference.entity.OperationLog;
import com.scut.conference.entity.SysUser;
import com.scut.conference.mapper.OperationLogMapper;
import com.scut.conference.mapper.UserMapper;
import com.scut.conference.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 操作审计日志:在关键业务动作完成后记录"谁在何时从哪个 IP 做了什么"。
 * 记录失败不影响主业务(吞掉异常,只打警告)。
 */
@Service
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;
    private final UserMapper userMapper;

    public OperationLogService(OperationLogMapper operationLogMapper, UserMapper userMapper) {
        this.operationLogMapper = operationLogMapper;
        this.userMapper = userMapper;
    }

    /** 从当前登录上下文取用户信息并写日志(自动获取请求 IP) */
    public void log(String action, String detail) {
        try {
            OperationLog entry = new OperationLog();
            entry.setUserId(UserContext.getUserId());
            entry.setRole(UserContext.getRole());
            entry.setAction(action);
            entry.setDetail(detail);
            entry.setIp(currentIp());
            if (entry.getUserId() != null) {
                SysUser user = userMapper.findById(entry.getUserId());
                entry.setUsername(user == null ? null : user.getUsername());
            }
            operationLogMapper.insert(entry);
        } catch (Exception e) {
            // 审计日志属于旁路功能,失败不阻断业务
            org.slf4j.LoggerFactory.getLogger(OperationLogService.class)
                    .warn("写入操作日志失败: {}", e.getMessage());
        }
    }

    /** 供登录等"请求中但上下文未建立"的场景显式指定用户 */
    public void log(String action, String detail, Long userId, String username, String role) {
        try {
            OperationLog entry = new OperationLog();
            entry.setUserId(userId);
            entry.setUsername(username);
            entry.setRole(role);
            entry.setAction(action);
            entry.setDetail(detail);
            entry.setIp(currentIp());
            operationLogMapper.insert(entry);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(OperationLogService.class)
                    .warn("写入操作日志失败: {}", e.getMessage());
        }
    }

    /** 管理端分页查询 */
    public Map<String, Object> listLogs(String username, String action, int page, int size) {
        String un = username == null ? "" : username;
        String ac = action == null ? "" : action;
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("list", operationLogMapper.listLogs(un, ac, (page - 1) * size, size));
        data.put("total", operationLogMapper.countLogs(un, ac));
        return data;
    }

    /** 通过 Spring 的 RequestContextHolder 取当前请求 IP(手写,不依赖工具类) */
    private String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip != null && !ip.isBlank()) {
                return ip.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return null;
    }
}
