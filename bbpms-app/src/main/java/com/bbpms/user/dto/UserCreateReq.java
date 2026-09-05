package com.bbpms.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "创建用户请求")
public class UserCreateReq {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50, message = "用户名需在 2~50 个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名仅支持字母/数字/下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码需在 6~64 位之间")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Size(min = 2, max = 50, message = "姓名需在 2~50 个字符之间")
    private String realName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱不能超过 100 个字符")
    private String email;

    @NotNull(message = "部门不能为空")
    private Long deptId;

    @NotNull(message = "用户类型不能为空")
    @Min(value = 1, message = "用户类型不合法")
    @Max(value = 3, message = "用户类型不合法")
    private Integer userType;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值不合法")
    @Max(value = 1, message = "状态值不合法")
    private Integer status;

    private List<Long> roleIds;
}
