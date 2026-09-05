package com.bbpms.user.controller;

import jakarta.validation.Valid;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bbpms.common.result.PageResp;
import com.bbpms.common.result.R;
import com.bbpms.user.dto.InstallerLocationDTO;
import com.bbpms.user.service.InstallerProfileService;
import com.bbpms.user.vo.InstallerVO;
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
@RequestMapping("/api/installers")
@RequiredArgsConstructor
public class InstallerController {

    private final InstallerProfileService installerService;

    @GetMapping("/online")
    @PreAuthorize("hasAuthority('installer:view')")
    public R<List<InstallerVO>> online() {
        return R.ok(installerService.getOnline());
    }

    @GetMapping("/locations")
    @PreAuthorize("hasAuthority('installer:view')")
    public R<List<InstallerVO>> locations() {
        return R.ok(installerService.listLocations());
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('installer:view')")
    public R<PageResp<InstallerVO>> page(@RequestParam(value = "keyword", required = false) String keyword,
                                         @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
                                         @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        Page<InstallerVO> p = installerService.pageInstallers(keyword, pageNum, pageSize);
        return R.ok(PageResp.of(p));
    }

    @GetMapping("/{id}/profile")
    @PreAuthorize("hasAuthority('installer:view')")
    public R<InstallerVO> profile(@PathVariable Long id) {
        return R.ok(installerService.getProfile(id));
    }

    @PostMapping("/location")
    @PreAuthorize("hasAuthority('installer:view')")
    public R<Void> updateLocation(@Valid @RequestBody InstallerLocationDTO dto) {
        installerService.updateLocation(dto);
        return R.ok();
    }
}