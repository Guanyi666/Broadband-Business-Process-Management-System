package com.bbpms.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bbpms.user.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /** The user's visible DIR/MENU rows (buttons excluded) via role-menu bindings. */
    @Select("""
            SELECT DISTINCT m.*
            FROM sys_menu m
            JOIN sys_role_menu rm ON m.id = rm.menu_id
            JOIN sys_user_role ur ON ur.role_id = rm.role_id
            JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0 AND r.status = 1
            WHERE ur.user_id = #{userId}
              AND m.deleted = 0 AND m.status = 1 AND m.type IN (1, 2)
            ORDER BY m.sort, m.id
            """)
    List<SysMenu> selectMenuTreeByUserId(@Param("userId") Long userId);

    /** Distinct permission codes for the user (any menu type that carries a perms). */
    @Select("""
            SELECT DISTINCT m.perms
            FROM sys_menu m
            JOIN sys_role_menu rm ON m.id = rm.menu_id
            JOIN sys_user_role ur ON ur.role_id = rm.role_id
            JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0 AND r.status = 1
            WHERE ur.user_id = #{userId}
              AND m.deleted = 0
              AND m.perms IS NOT NULL AND m.perms <> ''
            """)
    List<String> selectPermsByUserId(@Param("userId") Long userId);
}