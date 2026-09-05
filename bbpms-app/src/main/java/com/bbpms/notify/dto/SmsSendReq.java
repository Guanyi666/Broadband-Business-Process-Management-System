package com.bbpms.notify.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "短信发送请求")
public class SmsSendReq {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "模板编码不能为空")
    @Size(max = 64, message = "模板编码不能超过 64 个字符")
    private String templateCode;

    private Map<String, Object> params;
}