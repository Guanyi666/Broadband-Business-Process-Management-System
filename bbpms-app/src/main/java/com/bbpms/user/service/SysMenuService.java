package com.bbpms.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bbpms.user.dto.MenuCreateReq;
import com.bbpms.user.entity.SysMenu;
import com.bbpms.user.vo.SysMenuVO;

import java.util.List;

public interface SysMenuService extends IService<SysMenu> {

    Long create(MenuCreateReq req);

    void update(Long id, MenuCreateReq req);

    void delete(Long id);

    /** Complete management tree, including button permission nodes. */
    List<SysMenuVO> getAllMenuTree();

    List<SysMenuVO> getMenuTreeByUserId(Long userId);

    List<String> getPermsByUserId(Long userId);
}
