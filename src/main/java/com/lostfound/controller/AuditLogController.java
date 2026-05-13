package com.lostfound.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lostfound.common.Result;
import com.lostfound.service.AuditLogService;
import com.lostfound.vo.AuditLogVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/api/admin/audit-logs")
    public Result<IPage<AuditLogVO>> list(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(auditLogService.list(action, targetType, keyword, page, size));
    }
}
