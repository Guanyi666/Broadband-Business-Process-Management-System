package com.bbpms.dispatch.controller;

import jakarta.validation.Valid;
import com.bbpms.common.result.R;
import com.bbpms.dispatch.entity.DispatchRule;
import com.bbpms.dispatch.service.DispatchRuleService;
import com.bbpms.dispatch.vo.DispatchRuleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dispatch/rules")
@RequiredArgsConstructor
public class DispatchRuleController {

    private final DispatchRuleService ruleService;

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('dispatch:view')")
    public R<DispatchRuleVO> active() {
        return R.ok(ruleService.toVO(ruleService.getActive()));
    }

    @PutMapping("/")
    @PreAuthorize("hasAuthority('dispatch:rule:config')")
    public R<DispatchRuleVO> update(@Valid @RequestBody DispatchRule rule) {
        DispatchRule saved = ruleService.update(rule);
        return R.ok(ruleService.toVO(saved));
    }
}