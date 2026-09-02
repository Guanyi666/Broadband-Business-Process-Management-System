package com.bbpms.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseDO {

    private Long parentId;

    /** sys_menu columns are name / type / sort; entity names are kept via @TableField. */
    @TableField("name")
    private String menuName;

    @TableField("type")
    private String menuType;

    private String path;

    private String component;

    private String perms;

    private String icon;

    @TableField("sort")
    private Integer sortOrder;

    private Integer visible;

    private Integer status;
}