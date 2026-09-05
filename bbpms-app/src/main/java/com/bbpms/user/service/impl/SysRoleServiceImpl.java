package com.bbpms.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bbpms.common.enums.ResultCode;
import com.bbpms.common.exception.BizException;
import com.bbpms.user.dto.RoleCreateReq;
import com.bbpms.user.dto.RoleUpdateReq;
import com.bbpms.user.entity.SysRole;
import com.bbpms.user.entity.SysRoleMenu;
import com.bbpms.user.mapper.SysRoleMapper;
import com.bbpms.user.mapper.SysRoleMenuMapper;
import com.bbpms.user.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private final SysRoleMenuMapper roleMenuMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(RoleCreateReq req) {
        SysRole role = new SysRole();
        role.setCode(req.getCode());
        role.setName(req.getName());
        role.setDataScope(req.getDataScope());
        role.setRemark(req.getRemark());
        role.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        save(role);
        if (req.getMenuIds() != null && !req.getMenuIds().isEmpty()) {
            assignMenus(role.getId(), req.getMenuIds());
        }
        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "user:auth", allEntries = true)
    public void update(RoleUpdateReq req) {
        SysRole role = getById(req.getId());
        if (role == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "Role not found");
        }
        role.setName(req.getName());
        role.setDataScope(req.getDataScope());
        role.setRemark(req.getRemark());
        role.setStatus(req.getStatus());
        updateById(role);
        if (req.getMenuIds() != null) {
            assignMenus(role.getId(), req.getMenuIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "user:auth", allEntries = true)
    public void delete(Long id) {
        removeById(id);
        roleMenuMapper.deleteByRoleId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "user:auth", allEntries = true)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        roleMenuMapper.deleteByRoleId(roleId);
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        List<SysRoleMenu> list = menuIds.stream().map(mid -> {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(mid);
            return rm;
        }).collect(Collectors.toList());
        for (SysRoleMenu rm : list) {
            roleMenuMapper.insert(rm);
        }
    }

    @Override
    public List<Long> getMenuIds(Long roleId) {
        if (getById(roleId) == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "Role not found");
        }
        return roleMenuMapper.selectMenuIdsByRoleId(roleId);
    }

    @Override
    public List<SysRole> list() {
        // Was `return list()` — an infinite self-recursion (StackOverflowError).
        return list(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysRole>()
                .orderByAsc(SysRole::getSort));
    }

    @Override
    public SysRole getById(Long id) {
        return super.getById(id);
    }
}
