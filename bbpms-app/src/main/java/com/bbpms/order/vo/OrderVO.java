package com.bbpms.order.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Order view object. Returned to the client — never contains raw ciphertext.
 * {@code customerName} is the (masked) customer display name when joined.
 */
@Data
@Schema(description = "订单视图")
public class OrderVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "业务订单号")
    private String orderNo;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "客户姓名（已脱敏，按需填充）")
    private String customerName;

    @Schema(description = "套餐编码")
    private String packageCode;

    @Schema(description = "套餐名称")
    private String packageName;

    @Schema(description = "安装地址")
    private String installAddress;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "期望安装日期")
    private LocalDateTime expectedInstallDate;

    @Schema(description = "订单状态")
    private String status;

    @Schema(description = "订单来源（CS/CUSTOMER_SELF）")
    private String source;

    @Schema(description = "客服ID")
    private Long csId;

    @Schema(description = "审核员ID")
    private Long auditorId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "审核备注")
    private String auditRemark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "派单完成时间")
    private LocalDateTime dispatchTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "安装完成时间")
    private LocalDateTime completedTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "取消时间")
    private LocalDateTime cancelledTime;

    @Schema(description = "取消原因")
    private String cancelReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "资源房间ID（ITERATION 2 资源核查回写）")
    private Long roomId;

    @Schema(description = "资源核查状态（RESOURCE_OK/RESOURCE_INSUFFICIENT/NO_COVERAGE）")
    private String resourceStatus;

    @Schema(description = "资源核查备注")
    private String checkRemark;
}
