package com.bbpms.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class RoleUpdateReq {

    private Long id;

    private String name;

    private Integer dataScope;

    private String remark;

    private Integer status;

    private List<Long> menuIds;
}