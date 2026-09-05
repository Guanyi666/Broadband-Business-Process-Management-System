package com.bbpms.install.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "装机照片请求")
public class InstallPhotoReq {

    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    @NotBlank(message = "照片 objectKey 不能为空")
    @Size(max = 500, message = "照片 objectKey 不能超过 500 个字符")
    private String objectKey;

    @Size(max = 500, message = "照片 URL 不能超过 500 个字符")
    private String url;
}