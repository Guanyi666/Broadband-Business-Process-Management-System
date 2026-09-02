package com.bbpms.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bbpms.common.result.PageResp;
import com.bbpms.common.result.R;
import com.bbpms.user.dto.RoleCreateReq;
import com.bbpms.user.dto.RoleUpdateReq;
import com.bbpms.user.entity.SysRole;
import com.bbpms.user.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    @PostMapping
    @PreAuthorize("hasAuthority('system:role:add')")
    public R<Long> create(@RequestBody RoleCreateReq req) {
        return R.ok(roleService.create(req));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system:role:edit')")
    public R<Void> update(@RequestBody RoleUpdateReq req) {
        roleService.update(req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:delete')")
    public R<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return R.ok();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:view')")
    public R<SysRole> getById(@PathVariable Long id) {
        return R.ok(roleService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('system:role:view')")
    public R<List<SysRole>> list() {
        return R.ok(roleService.list());
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:role:view')")
    public R<PageResp<SysRole>> page(@RequestParam(value = "keyword", required = false) String keyword,
                                     @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
                                     @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        LambdaQueryWrapper<SysRole> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(SysRole::getName, keyword).or().like(SysRole::getCode, keyword));
        }
        qw.orderByAsc(SysRole::getSort);
        return R.ok(PageResp.of(roleService.page(new Page<>(pageNum, pageSize), qw)));
    }

    @PostMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('system:role:assign')")
    public R<Void> assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        roleService.assignMenus(id, menuIds);
        return R.ok();
    }
}