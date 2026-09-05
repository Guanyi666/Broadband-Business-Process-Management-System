package com.bbpms.install.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "客户签名请求")
public class InstallSignatureReq {

    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    @NotBlank(message = "客户姓名不能为空")
    @Size(min = 2, max = 50, message = "客户姓名需在 2~50 个字符之间")
    private String customerName;

    @Size(max = 1024, message = "签名数据过长")
    private String dataUrl;

    @NotBlank(message = "签名图片不能为空")
    @Size(max = 500, message = "签名图片 key 不能超过 500 个字符")
    private String objectKey;
}