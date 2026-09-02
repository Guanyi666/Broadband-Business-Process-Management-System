package com.bbpms.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserUpdateReq {

    private Long id;

    private String realName;

    private String phone;

    private String email;

    private Long deptId;

    private Integer status;

    private List<Long> roleIds;
}