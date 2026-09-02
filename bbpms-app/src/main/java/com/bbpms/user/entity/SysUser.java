package com.bbpms.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseDO {

    private String username;

    private String password;

    private String realName;

    /** Display name; the column exists in the seed but was unmapped. */
    private String nickname;

    private String phone;

    @TableField("phone_enc")
    private String phoneEnc;

    private String email;

    @TableField("id_card_no_enc")
    private String idCardNoEnc;

    private String avatar;

    private Integer gender;

    private LocalDate birthday;

    private Long deptId;

    private Integer userType;

    private Integer status;

    /** Table column is last_login_time (entity name kept via @TableField). */
    @TableField("last_login_time")
    private LocalDateTime lastLoginAt;

    private String lastLoginIp;

    private Long tenantId;
}