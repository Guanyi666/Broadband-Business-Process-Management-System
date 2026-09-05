package com.bbpms.user.controller;

import jakarta.validation.Valid;
import com.bbpms.common.result.PageResp;
import com.bbpms.common.result.R;
import com.bbpms.user.dto.PasswordChangeReq;
import com.bbpms.user.dto.UserCreateReq;
import com.bbpms.user.dto.UserPageReq;
import com.bbpms.user.dto.UserUpdateReq;
import com.bbpms.user.entity.SysUser;
import com.bbpms.user.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    @PostMapping
    @PreAuthorize("hasAuthority('system:user:add')")
    public R<Long> create(@Valid @RequestBody UserCreateReq req) {
        return R.ok(userService.create(req));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system:user:edit')")
    public R<Void> update(@Valid @RequestBody UserUpdateReq req) {
        userService.update(req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    public R<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return R.ok();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:view')")
    public R<SysUser> getById(@PathVariable Long id) {
        return R.ok(userService.getById(id));
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:user:view')")
    public R<PageResp<SysUser>> page(UserPageReq req) {
        return R.ok(userService.page(req));
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('system:user:assign')")
    public R<Void> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        userService.assignRoles(id, roleIds);
        return R.ok();
    }

    @PostMapping("/{id}/password")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public R<Void> changePassword(@PathVariable Long id, @Valid @RequestBody PasswordChangeReq req) {
        userService.changePassword(id, req);
        return R.ok();
    }
}