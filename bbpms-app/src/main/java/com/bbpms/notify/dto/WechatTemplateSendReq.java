package com.bbpms.notify.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.Map;
@Data
@Schema(description = "微信模板消息发送请求")
public class WechatTemplateSendReq {
    @Schema(description = "接收人 openId")
    @NotBlank(message = "openId 不能为空")
    @Size(max = 64, message = "openId 不能超过 64 个字符")
    private String openId;
    @Schema(description = "模板ID")
    @NotBlank(message = "templateId 不能为空")
    @Size(max = 64, message = "templateId 不能超过 64 个字符")
    private String templateId;
    @Schema(description = "模板参数")
    private Map<String, Object> params;
    @Schema(description = "跳转链接")
    @Size(max = 500, message = "跳转链接不能超过 500 个字符")
    private String url;
}
