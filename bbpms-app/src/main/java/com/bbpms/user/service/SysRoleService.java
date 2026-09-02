package com.bbpms.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bbpms.user.dto.RoleCreateReq;
import com.bbpms.user.dto.RoleUpdateReq;
import com.bbpms.user.entity.SysRole;

import java.util.List;

public interface SysRoleService extends IService<SysRole> {

    Long create(RoleCreateReq req);

    void update(RoleUpdateReq req);

    void delete(Long id);

    void assignMenus(Long roleId, List<Long> menuIds);

    List<SysRole> list();

    SysRole getById(Long id);
}