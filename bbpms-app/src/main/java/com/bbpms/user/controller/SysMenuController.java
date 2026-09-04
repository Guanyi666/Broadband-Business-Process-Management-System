package com.bbpms.user.controller;

import com.bbpms.common.result.R;
import com.bbpms.common.util.SecurityUtils;
import com.bbpms.user.dto.MenuCreateReq;
import com.bbpms.user.service.SysMenuService;
import com.bbpms.user.vo.SysMenuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService menuService;

    @PostMapping
    @PreAuthorize("hasAuthority('system:menu:add')")
    public R<Long> create(@RequestBody MenuCreateReq req) {
        return R.ok(menuService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:edit')")
    public R<Void> update(@PathVariable Long id, @RequestBody MenuCreateReq req) {
        menuService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:delete')")
    public R<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return R.ok();
    }

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:menu:view')")
    public R<List<SysMenuVO>> tree() {
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(menuService.getMenuTreeByUserId(userId));
    }

    @GetMapping("/perms")
    @PreAuthorize("hasAuthority('system:menu:view')")
    public R<List<String>> perms() {
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(menuService.getPermsByUserId(userId));
    }
}