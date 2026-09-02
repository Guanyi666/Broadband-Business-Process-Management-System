package com.bbpms.log.controller;
import com.bbpms.common.result.PageResp;
import com.bbpms.common.result.R;
import com.bbpms.log.dto.LoginLogPageReq;
import com.bbpms.log.entity.LoginLog;
import com.bbpms.log.service.LoginLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/logs/login")
@RequiredArgsConstructor
public class LoginLogController {
    private final LoginLogService logService;
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('log:view')")
    public R<PageResp<LoginLog>> page(LoginLogPageReq req) {
        return R.ok(logService.page(req));
    }
}