package com.bbpms.user.controller;

import com.bbpms.common.result.R;
import com.bbpms.user.entity.SysDept;
import com.bbpms.user.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/depts")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptService deptService;

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:dept:view')")
    public R<List<SysDept>> tree() {
        return R.ok(deptService.getTree());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:dept:add')")
    public R<Boolean> create(@RequestBody SysDept dept) {
        return R.ok(deptService.save(dept));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept:edit')")
    public R<Boolean> update(@PathVariable Long id, @RequestBody SysDept dept) {
        dept.setId(id);
        return R.ok(deptService.updateById(dept));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept:delete')")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(deptService.removeById(id));
    }
}