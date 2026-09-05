package com.bbpms.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "修改密码请求")
public class PasswordChangeReq {

    @NotBlank(message = "原密码不能为空")
    @Size(max = 64, message = "密码长度不能超过 64 位")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "新密码需在 6~64 位之间")
    private String newPassword;
}