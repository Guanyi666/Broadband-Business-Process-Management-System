package com.bbpms.log.entity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper = true) @TableName("login_log")
public class LoginLog extends BaseDO {
    @TableField("user_id") private Long userId;
    @TableField("username") private String username;
    @TableField("ip") private String ip;
    @TableField("user_agent") private String userAgent;
    @TableField("status") private Integer status;
    @TableField("message") private String message;
}