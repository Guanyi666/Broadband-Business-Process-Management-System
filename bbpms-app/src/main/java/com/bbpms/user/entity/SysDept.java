package com.bbpms.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDept extends BaseDO {

    /** Aligned to sys_dept table columns (name/path/sort) — the previous
     *  deptName/deptCode/ancestors/sortOrder fields mapped to columns that do
     *  not exist. */
    private Long parentId;

    private String name;

    private String leader;

    private String phone;

    private String path;

    private Integer sort;

    private Integer status;
}