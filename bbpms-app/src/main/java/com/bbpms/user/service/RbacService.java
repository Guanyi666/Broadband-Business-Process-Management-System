package com.bbpms.user.service;

import com.bbpms.user.entity.SysMenu;
import com.bbpms.user.vo.SysMenuVO;

import java.util.List;

public interface RbacService {

    List<SysMenuVO> buildMenuTree(List<SysMenu> flat);

    Integer getCurrentDataScope(Long userId);
}