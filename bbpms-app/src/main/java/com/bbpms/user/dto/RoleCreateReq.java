package com.bbpms.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "创建角色请求")
public class RoleCreateReq {

    @NotBlank(message = "角色编码不能为空")
    @Size(min = 2, max = 50, message = "角色编码需在 2~50 个字符之间")
    @Pattern(regexp = "^[A-Za-z0-9_:]+$", message = "角色编码仅支持字母/数字/冒号/下划线")
    private String code;

    @NotBlank(message = "角色名称不能为空")
    @Size(min = 2, max = 50, message = "角色名称需在 2~50 个字符之间")
    private String name;

    @Min(value = 1, message = "数据权限范围不合法")
    @Max(value = 5, message = "数据权限范围不合法")
    private Integer dataScope;

    @Size(max = 200, message = "备注不能超过 200 个字符")
    private String remark;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值不合法")
    @Max(value = 1, message = "状态值不合法")
    private Integer status;

    private List<Long> menuIds;
}