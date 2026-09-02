package com.bbpms.install.controller;

import com.bbpms.common.result.PageResp;
import com.bbpms.common.result.R;
import com.bbpms.common.util.SecurityUtils;
import com.bbpms.install.dto.InstallArriveReq;
import com.bbpms.install.dto.InstallCompleteReq;
import com.bbpms.install.dto.InstallInfoReq;
import com.bbpms.install.dto.InstallPageReq;
import com.bbpms.install.dto.InstallPhotoReq;
import com.bbpms.install.dto.InstallSignatureReq;
import com.bbpms.install.service.InstallService;
import com.bbpms.install.vo.InstallProgressVO;
import com.bbpms.install.vo.InstallRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/install")
@RequiredArgsConstructor
public class InstallController {

    private final InstallService installService;

    @PostMapping("/{workOrderId}/arrive")
    @PreAuthorize("hasAuthority('install:arrive')")
    public R<Void> arrive(@PathVariable("workOrderId") Long workOrderId,
                          @RequestBody InstallArriveReq req) {
        req.setWorkOrderId(workOrderId);
        installService.arrive(workOrderId, req, SecurityUtils.getCurrentUserId());
        return R.ok();
    }

    @PostMapping("/{workOrderId}/info")
    @PreAuthorize("hasAuthority('install:info')")
    public R<Void> saveInfo(@PathVariable("workOrderId") Long workOrderId,
                            @RequestBody InstallInfoReq req) {
        req.setWorkOrderId(workOrderId);
        installService.saveInfo(workOrderId, req);
        return R.ok();
    }

    @PostMapping("/{workOrderId}/photos")
    @PreAuthorize("hasAuthority('install:photo')")
    public R<Void> addPhoto(@PathVariable("workOrderId") Long workOrderId,
                            @RequestBody InstallPhotoReq req) {
        req.setWorkOrderId(workOrderId);
        installService.addPhoto(workOrderId, req);
        return R.ok();
    }

    @PostMapping("/{workOrderId}/signature")
    @PreAuthorize("hasAuthority('install:sign')")
    public R<Void> addSignature(@PathVariable("workOrderId") Long workOrderId,
                                @RequestBody InstallSignatureReq req) {
        req.setWorkOrderId(workOrderId);
        installService.addSignature(workOrderId, req);
        return R.ok();
    }

    @PostMapping("/{workOrderId}/complete")
    @PreAuthorize("hasAuthority('install:complete')")
    public R<Void> complete(@PathVariable("workOrderId") Long workOrderId,
                            @RequestBody InstallCompleteReq req) {
        req.setWorkOrderId(workOrderId);
        installService.complete(workOrderId, req, SecurityUtils.getCurrentUserId());
        return R.ok();
    }

    @GetMapping("/by-work-order/{workOrderId}")
    @PreAuthorize("hasAuthority('install:view')")
    public R<InstallRecordVO> getByWorkOrder(@PathVariable("workOrderId") Long workOrderId) {
        return R.ok(installService.getByWorkOrderId(workOrderId));
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('install:view')")
    public R<PageResp<InstallRecordVO>> page(InstallPageReq req) {
        return R.ok(installService.page(req));
    }

    @GetMapping("/my")
    public R<List<InstallRecordVO>> mine() {
        Long uid = SecurityUtils.requireUserId();
        return R.ok(installService.getByInstaller(uid));
    }

    @GetMapping("/progress/{workOrderId}")
    @PreAuthorize("hasAuthority('install:view')")
    public R<InstallProgressVO> progress(@PathVariable("workOrderId") Long workOrderId) {
        return R.ok(installService.getProgress(workOrderId));
    }
}