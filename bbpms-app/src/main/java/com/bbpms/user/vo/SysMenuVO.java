package com.bbpms.user.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu tree node as returned to the admin UI. Field names match the frontend
 * {@code MenuNode} type (name / type / perm / sort) — the previous
 * menuName/menuType/sortOrder/perms names did not map and broke the sidebar.
 */
@Data
public class SysMenuVO {

    private Long id;

    private Long parentId;

    private String name;

    private Integer type;

    private String path;

    private String component;

    private String perm;

    private String icon;

    private Integer sort;

    private Integer visible;

    private Integer status;

    private List<SysMenuVO> children = new ArrayList<>();
}
