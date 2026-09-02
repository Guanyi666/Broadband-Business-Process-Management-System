package com.bbpms.user.vo;

import lombok.Data;

import java.util.List;

@Data
public class SysRoleVO {

    private Long id;

    private String roleCode;

    private String roleName;

    private Integer dataScope;

    private String description;

    private Integer status;

    private List<Long> menuIds;
}