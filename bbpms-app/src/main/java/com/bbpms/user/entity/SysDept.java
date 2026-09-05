package com.bbpms.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDept extends BaseDO {

    /** Aligned to sys_dept table columns (name/path/sort) — the previous
     *  deptName/deptCode/ancestors/sortOrder fields mapped to columns that do
     *  not exist. */
    @Schema(description = "父部门ID，0 为顶级")
    private Long parentId;

    @Schema(description = "部门名称")
    @NotBlank(message = "部门名称不能为空")
    @Size(max = 50, message = "部门名称不能超过 50 个字符")
    private String name;

    @Schema(description = "负责人")
    @Size(max = 50, message = "负责人不能超过 50 个字符")
    private String leader;

    @Schema(description = "联系电话")
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "部门路径")
    @Size(max = 200, message = "部门路径不能超过 200 个字符")
    private String path;

    @Schema(description = "排序")
    @Min(value = 0, message = "排序不能为负数")
    @Max(value = 9999, message = "排序不能超过 9999")
    private Integer sort;

    @Schema(description = "状态：0 停用，1 启用")
    @Min(value = 0, message = "状态只能为 0 或 1")
    @Max(value = 1, message = "状态只能为 0 或 1")
    private Integer status;
}