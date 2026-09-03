package com.bbpms.user.dto;

import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.util.List;

@Alias("UserUserAuthInfoDTO")
@Data
public class UserAuthInfoDTO {

    private Long userId;

    private String username;

    private Integer status;

    private Integer userType;

    private List<String> roles;

    private List<String> permissions;

    private Integer dataScope;

    /** Department of the user (sys_user.dept_id) — carried into the JWT so the
     *  DataScopeInnerInterceptor can build DEPT / DEPT_AND_CHILD filters later. */
    private Long deptId;
}