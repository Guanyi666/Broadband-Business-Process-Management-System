package com.bbpms.notify.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "消息模板创建/更新请求")
public class MessageTemplateCreateReq {

    private Long id;

    @NotBlank(message = "模板编码不能为空")
    @Size(max = 64, message = "模板编码不能超过 64 个字符")
    private String code;

    @NotBlank(message = "渠道不能为空")
    @Size(max = 32, message = "渠道不能超过 32 个字符")
    private String channel;

    @Size(max = 200, message = "主题不能超过 200 个字符")
    private String subject;

    @NotBlank(message = "内容不能为空")
    @Size(max = 2000, message = "内容不能超过 2000 个字符")
    private String content;

    @Size(max = 64, message = "阿里云模板 ID 过长")
    private String aliyunTemplateId;

    @Size(max = 64, message = "微信模板 ID 过长")
    private String wechatTemplateId;

    @NotNull(message = "启用状态不能为空")
    @Min(value = 0, message = "状态值不合法")
    @Max(value = 1, message = "状态值不合法")
    private Integer enabled;
}