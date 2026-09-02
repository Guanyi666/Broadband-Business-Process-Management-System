package com.bbpms.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserCreateReq {

    private String username;

    private String password;

    private String realName;

    private String phone;

    private String email;

    private Long deptId;

    private Integer userType;

    private Integer status;

    private List<Long> roleIds;
}