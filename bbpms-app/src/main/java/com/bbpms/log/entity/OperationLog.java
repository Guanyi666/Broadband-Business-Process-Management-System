package com.bbpms.log.entity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper = true) @TableName("operation_log")
public class OperationLog extends BaseDO {
    @TableField("user_id") private Long userId;
    @TableField("username") private String username;
    @TableField("module") private String module;
    @TableField("action") private String action;
    @TableField("request_uri") private String requestUri;
    @TableField("method") private String method;
    @TableField("params") private String params;
    @TableField("result") private String result;
    @TableField("ip") private String ip;
    @TableField("user_agent") private String userAgent;
    @TableField("cost_ms") private Integer costMs;
    @TableField("status") private Integer status;
    @TableField("error") private String error;
}