package com.bbpms.user.service.impl;

import com.bbpms.user.entity.SysMenu;
import com.bbpms.user.vo.SysMenuVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RbacServiceImplTest {

    private final RbacServiceImpl service = new RbacServiceImpl(null);

    @Test
    void buildsAndSortsCompleteManagementTreeIncludingButtons() {
        List<SysMenuVO> roots = service.buildMenuTree(List.of(
                menu(102L, 21L, "取消", "3", 2),
                menu(21L, 20L, "订单列表", "2", 1),
                menu(20L, 0L, "订单管理", "1", 2),
                menu(1L, 0L, "数据看板", "2", 1),
                menu(101L, 21L, "审核", "3", 1)
        ));

        assertEquals(List.of(1L, 20L), roots.stream().map(SysMenuVO::getId).toList());
        SysMenuVO orderMenu = roots.get(1);
        assertEquals(21L, orderMenu.getChildren().get(0).getId());
        assertEquals(List.of(101L, 102L), orderMenu.getChildren().get(0).getChildren()
                .stream().map(SysMenuVO::getId).toList());
        assertEquals(3, orderMenu.getChildren().get(0).getChildren().get(0).getType());
    }

    private static SysMenu menu(Long id, Long parentId, String name, String type, int sort) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setParentId(parentId);
        menu.setMenuName(name);
        menu.setMenuType(type);
        menu.setSortOrder(sort);
        return menu;
    }
}
