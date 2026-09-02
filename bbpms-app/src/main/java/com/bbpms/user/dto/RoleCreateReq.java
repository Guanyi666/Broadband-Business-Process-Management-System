package com.bbpms.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class RoleCreateReq {

    private String code;

    private String name;

    private Integer dataScope;

    private String remark;

    private Integer status;

    private List<Long> menuIds;
}