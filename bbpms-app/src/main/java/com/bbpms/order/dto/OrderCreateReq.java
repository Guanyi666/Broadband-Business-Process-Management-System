package com.bbpms.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "客户ID不能为空")
    @Schema(description = "客户ID")
    private Long customerId;

    @NotBlank(message = "套餐编码不能为空")
    @Schema(description = "套餐编码")
    private String packageCode;

    @Schema(description = "套餐名称（冗余存储）")
    private String packageName;

    @NotBlank(message = "安装地址不能为空")
    @Schema(description = "安装地址")
    private String installAddress;

    @Schema(description = "客户期望安装日期")
    private LocalDateTime expectedInstallDate;

    @Schema(description = "预约上门时间")
    private LocalDateTime appointmentTime;

    @Schema(description = "联系手机号")
    private String contactPhone;

    @Schema(description = "备注")
    private String remark;

    /** 资源核查结果回写（ITERATION 2）：房间ID（可选，下单前经 /resources/check 获得）。 */
    @Schema(description = "资源房间ID")
    private Long roomId;

    @Schema(description = "资源核查结果（RESOURCE_OK/RESOURCE_INSUFFICIENT/NO_COVERAGE，可选）")
    private String resourceStatus;

    @Schema(description = "资源核查备注（可选）")
    private String checkRemark;
}
