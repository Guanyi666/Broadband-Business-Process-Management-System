package com.bbpms.user.dto;

import com.bbpms.common.entity.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserPageReq extends BaseDTO {

    private Long deptId;

    private Integer userType;

    private Integer status;

    private String realName;
}