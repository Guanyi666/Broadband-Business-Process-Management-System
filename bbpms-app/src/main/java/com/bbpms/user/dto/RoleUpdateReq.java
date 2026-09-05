package com.bbpms.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "更新角色请求")
public class RoleUpdateReq {

    @NotNull(message = "角色ID不能为空")
    private Long id;

    @Size(min = 2, max = 50, message = "角色名称需在 2~50 个字符之间")
    private String name;

    @Min(value = 1, message = "数据权限范围不合法")
    @Max(value = 5, message = "数据权限范围不合法")
    private Integer dataScope;

    @Size(max = 200, message = "备注不能超过 200 个字符")
    private String remark;

    @Min(value = 0, message = "状态值不合法")
    @Max(value = 1, message = "状态值不合法")
    private Integer status;

    private List<Long> menuIds;
}