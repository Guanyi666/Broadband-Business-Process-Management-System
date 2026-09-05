package com.bbpms.customerportal.controller;

import com.bbpms.common.result.PageResp;
import com.bbpms.common.result.R;
import com.bbpms.customerportal.dto.PackageSaveReq;
import com.bbpms.customerportal.service.PackageAdminService;
import com.bbpms.customerportal.vo.PackageAdminVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 套餐资源管理（后台「资源管理 → 套餐资源」）。
 *
 * <p>权限：resource:view 查看，resource:edit 编辑。</p>
 */
@Tag(name = "package-admin", description = "套餐资源管理")
@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
public class PackageAdminController {

    private final PackageAdminService packageAdminService;

    @Operation(summary = "套餐分页列表")
    @GetMapping
    @PreAuthorize("hasAuthority('resource:view')")
    public R<PageResp<PackageAdminVO>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return R.ok(packageAdminService.page(pageNum, pageSize, keyword, status));
    }

    @Operation(summary = "启用中的套餐（下拉选择）")
    @GetMapping("/enabled")
    @PreAuthorize("hasAuthority('resource:view')")
    public R<List<PackageAdminVO>> enabled() {
        return R.ok(packageAdminService.listEnabled());
    }

    @Operation(summary = "新增套餐")
    @PostMapping
    @PreAuthorize("hasAuthority('resource:edit')")
    public R<Long> create(@Valid @RequestBody PackageSaveReq req) {
        return R.ok(packageAdminService.create(req));
    }

    @Operation(summary = "编辑套餐")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('resource:edit')")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody PackageSaveReq req) {
        packageAdminService.update(id, req);
        return R.ok();
    }

    @Operation(summary = "删除套餐")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('resource:edit')")
    public R<Void> delete(@PathVariable Long id) {
        packageAdminService.delete(id);
        return R.ok();
    }

    @Operation(summary = "启用/禁用套餐")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('resource:edit')")
    public R<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        packageAdminService.toggleStatus(id, status);
        return R.ok();
    }
}
