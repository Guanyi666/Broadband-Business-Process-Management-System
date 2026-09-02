package com.bbpms.user.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SysUserVO {

    private Long id;

    private String username;

    private String realName;

    private String phone;

    private String email;

    private Long deptId;

    private Integer userType;

    private Integer status;

    private LocalDateTime lastLoginAt;

    private String lastLoginIp;

    private List<String> roleCodes;

    private List<Long> roleIds;
}