package com.bbpms.log.controller;
import com.bbpms.common.result.PageResp;
import com.bbpms.common.result.R;
import com.bbpms.log.dto.OperationLogPageReq;
import com.bbpms.log.entity.OperationLog;
import com.bbpms.log.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/logs/operation")
@RequiredArgsConstructor
public class OperationLogController {
    private final OperationLogService logService;
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('log:view')")
    public R<PageResp<OperationLog>> page(OperationLogPageReq req) {
        return R.ok(logService.page(req));
    }
}