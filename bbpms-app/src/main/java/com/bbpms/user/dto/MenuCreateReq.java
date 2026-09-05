package com.bbpms.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建菜单请求")
public class MenuCreateReq {

    private Long parentId;

    @NotBlank(message = "菜单名称不能为空")
    @Size(min = 1, max = 50, message = "菜单名称需在 1~50 个字符之间")
    private String menuName;

    @NotBlank(message = "菜单类型不能为空")
    @Pattern(regexp = "^(M|C|F|A)$", message = "菜单类型不合法（M目录/C菜单/F按钮/A接口）")
    private String menuType;

    @Size(max = 200, message = "路由路径不能超过 200 个字符")
    private String path;

    @Size(max = 200, message = "组件路径不能超过 200 个字符")
    private String component;

    @Size(max = 100, message = "权限标识不能超过 100 个字符")
    private String perms;

    @Size(max = 50, message = "图标名不能超过 50 个字符")
    private String icon;

    @Min(value = 0, message = "排序不能为负数")
    @Max(value = 9999, message = "排序不能超过 9999")
    private Integer sortOrder;

    @NotNull(message = "可见性不能为空")
    @Min(value = 0, message = "可见性值不合法")
    @Max(value = 1, message = "可见性值不合法")
    private Integer visible;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值不合法")
    @Max(value = 1, message = "状态值不合法")
    private Integer status;
}