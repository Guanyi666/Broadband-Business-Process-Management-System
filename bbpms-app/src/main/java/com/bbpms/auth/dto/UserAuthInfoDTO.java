package com.bbpms.auth.dto;

import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.util.List;

@Alias("AuthUserAuthInfoDTO")
@Data
public class UserAuthInfoDTO {

    private Long userId;

    private String username;

    private Integer status;

    private Integer userType;

    private List<String> roles;

    private List<String> permissions;

    private Integer dataScope;
}