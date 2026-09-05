package com.bbpms.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Payload for creating a broadband order.
 *
 * <p>The order is created in {@link com.bbpms.common.enums.OrderStatus#CREATED}
 * — a separate audit action is required to move it to AUDITED.</p>
 */
@Data
@Schema(description = "创建订单请求")
public class OrderCreateReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 中国大陆手机号（13x/14x/15x/16x/17x/18x/19x 开头，11 位） */
    public static final String PHONE_REGEX = "^1[3-9]\\d{9}$";

    @NotNull(message = "客户ID不能为空")
    @Schema(description = "客户ID")
    private Long customerId;

    @NotBlank(message = "套餐编码不能为空")
    @Size(max = 64, message = "套餐编码不能超过 64 个字符")
    @Schema(description = "套餐编码")
    private String packageCode;

    @Schema(description = "套餐名称（冗余存储）")
    @Size(max = 128, message = "套餐名称不能超过 128 个字符")
    private String packageName;

    @NotBlank(message = "安装地址不能为空")
    @Size(max = 200, message = "安装地址不能超过 200 个字符")
    @Schema(description = "安装地址")
    private String installAddress;

    @Schema(description = "客户期望安装日期")
    @Future(message = "期望安装日期不能早于当前时间")
    private LocalDateTime expectedInstallDate;

    @Schema(description = "预约上门时间")
    @Future(message = "预约上门时间不能早于当前时间")
    private LocalDateTime appointmentTime;

    @Schema(description = "联系手机号")
    @Pattern(regexp = PHONE_REGEX, message = "手机号格式不正确")
    private String contactPhone;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;

    /** 资源核查结果回写（ITERATION 2）：房间ID（可选，下单前经 /resources/check 获得）。 */
    @Schema(description = "资源房间ID")
    private Long roomId;

    @Schema(description = "资源核查结果（RESOURCE_OK/RESOURCE_INSUFFICIENT/NO_COVERAGE，可选）")
    private String resourceStatus;

    @Schema(description = "资源核查备注（可选）")
    @Size(max = 500, message = "资源核查备注不能超过 500 个字符")
    private String checkRemark;
}
