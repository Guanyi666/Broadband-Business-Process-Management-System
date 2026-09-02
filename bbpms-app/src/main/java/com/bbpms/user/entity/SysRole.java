package com.bbpms.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseDO {

    /** Table column is `code`; the old roleCode/roleName/description fields mapped
     *  to non-existent columns (role_code/role_name/description) and every
     *  sys_role query failed with Unknown column. */
    private String code;

    private String name;

    private Integer dataScope;

    private Integer sort;

    private String remark;

    private Integer status;
}