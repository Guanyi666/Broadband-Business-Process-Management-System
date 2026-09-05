package com.bbpms.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 资源核查请求。 */
@Data
@Schema(description = "资源核查请求")
public class ResourceCheckReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "安装地址不能为空")
    @Size(max = 200, message = "安装地址不能超过 200 个字符")
    @Schema(description = "安装地址（小区/楼栋/单元/房号文本）")
    private String address;

    @Schema(description = "房号（可选，默认取地址尾部）")
    @Size(max = 50, message = "房号不能超过 50 个字符")
    private String roomNo;
}