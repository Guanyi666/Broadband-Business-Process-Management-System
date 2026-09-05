package com.bbpms.dispatch.controller;

import com.bbpms.common.result.PageResp;
import com.bbpms.common.result.R;
import com.bbpms.dispatch.dto.CandidateDTO;
import com.bbpms.dispatch.dto.DispatchQueryReq;
import com.bbpms.dispatch.dto.DispatchResultDTO;
import com.bbpms.dispatch.dto.ManualDispatchReq;
import com.bbpms.dispatch.dto.ReassignReq;
import com.bbpms.dispatch.service.DispatchService;
import com.bbpms.dispatch.vo.DispatchRecordVO;
import com.bbpms.dispatch.vo.DispatchStatVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dispatch")
@RequiredArgsConstructor
public class DispatchController {

    private final DispatchService dispatchService;

    /** System-internal auto-dispatch (called by OrderAuditedListener). */
    @PostMapping("/auto")
    public R<DispatchResultDTO> autoDispatch(@RequestParam Long orderId) {
        return R.ok(dispatchService.autoDispatch(orderId));
    }

    @PostMapping("/manual")
    @PreAuthorize("hasAuthority('dispatch:manual')")
    public R<DispatchResultDTO> manualDispatch(@RequestBody ManualDispatchReq req) {
        return R.ok(dispatchService.manualDispatch(req));
    }

    @PostMapping("/{id}/reassign")
    @PreAuthorize("hasAuthority('dispatch:reassign')")
    public R<DispatchResultDTO> reassign(@PathVariable("id") Long workOrderId,
                                         @RequestBody ReassignReq req) {
        req.setWorkOrderId(workOrderId);
        return R.ok(dispatchService.reassign(req));
    }

    @GetMapping("/candidates")
    @PreAuthorize("hasAuthority('dispatch:view')")
    public R<List<CandidateDTO>> candidates(
            @RequestParam Long orderId,
            @RequestParam(value = "excludeInstallerId", required = false) Long excludeInstallerId,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return R.ok(dispatchService.getCandidates(orderId, excludeInstallerId, limit));
    }

    @GetMapping("/records/page")
    @PreAuthorize("hasAuthority('dispatch:view')")
    public R<PageResp<DispatchRecordVO>> pageRecords(DispatchQueryReq req) {
        return R.ok(dispatchService.pageRecords(req));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('dispatch:view')")
    public R<DispatchStatVO> stats(@RequestParam(value = "days", required = false) Integer days) {
        return R.ok(dispatchService.stats(days));
    }
}
