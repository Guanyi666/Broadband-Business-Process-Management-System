package com.bbpms.auth.dto;

import lombok.Data;

@Data
public class SysUserDTO {

    private Long id;

    private String username;

    private Integer status;
}