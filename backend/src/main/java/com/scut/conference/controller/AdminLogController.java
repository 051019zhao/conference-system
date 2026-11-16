package com.scut.conference.controller;

import com.scut.conference.common.Result;
import com.scut.conference.security.RequireRole;
import com.scut.conference.service.OperationLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 管理后台:操作审计日志查询 */
@RestController
@RequestMapping("/api/admin")
@RequireRole("ADMIN")
public class AdminLogController {

    private final OperationLogService operationLogService;

    public AdminLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping("/logs")
    public Result<?> logs(@RequestParam(required = false) String username,
                          @RequestParam(required = false) String action,
                          @RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return Result.ok(operationLogService.listLogs(username, action, Math.max(page, 1), safeSize));
    }
}
