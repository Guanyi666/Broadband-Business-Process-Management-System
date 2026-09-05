package com.bbpms.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bbpms.user.dto.MenuCreateReq;
import com.bbpms.user.entity.SysMenu;
import com.bbpms.user.mapper.SysMenuMapper;
import com.bbpms.user.service.RbacService;
import com.bbpms.user.service.SysMenuService;
import com.bbpms.user.vo.SysMenuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    private final RbacService rbacService;

    @Override
    public Long create(MenuCreateReq req) {
        SysMenu menu = new SysMenu();
        menu.setParentId(req.getParentId() == null ? 0L : req.getParentId());
        menu.setMenuName(req.getMenuName());
        menu.setMenuType(req.getMenuType());
        menu.setPath(req.getPath());
        menu.setComponent(req.getComponent());
        menu.setPerms(req.getPerms());
        menu.setIcon(req.getIcon());
        menu.setSortOrder(req.getSortOrder());
        menu.setVisible(req.getVisible());
        menu.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        save(menu);
        return menu.getId();
    }

    @Override
    @CacheEvict(value = "user:auth", allEntries = true)
    public void update(Long id, MenuCreateReq req) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setParentId(req.getParentId() == null ? 0L : req.getParentId());
        menu.setMenuName(req.getMenuName());
        menu.setMenuType(req.getMenuType());
        menu.setPath(req.getPath());
        menu.setComponent(req.getComponent());
        menu.setPerms(req.getPerms());
        menu.setIcon(req.getIcon());
        menu.setSortOrder(req.getSortOrder());
        menu.setVisible(req.getVisible());
        menu.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        updateById(menu);
    }

    @Override
    @CacheEvict(value = "user:auth", allEntries = true)
    public void delete(Long id) {
        removeById(id);
    }

    @Override
    public List<SysMenuVO> getAllMenuTree() {
        return rbacService.buildMenuTree(baseMapper.selectAllForManagement());
    }

    @Override
    public List<SysMenuVO> getMenuTreeByUserId(Long userId) {
        List<SysMenu> menus = baseMapper.selectMenuTreeByUserId(userId);
        return rbacService.buildMenuTree(menus);
    }

    @Override
    public List<String> getPermsByUserId(Long userId) {
        return baseMapper.selectPermsByUserId(userId);
    }
}
