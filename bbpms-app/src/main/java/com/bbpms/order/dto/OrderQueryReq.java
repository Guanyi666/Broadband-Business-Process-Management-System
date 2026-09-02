package com.bbpms.order.dto;

import com.bbpms.common.entity.BaseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Paginated query for orders. Subject to {@link com.bbpms.common.annotation.DataScope}
 * via the mapper.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "订单分页查询请求")
public class OrderQueryReq extends BaseDTO {

    @Schema(description = "订单编号（精确匹配）")
    private String orderNo;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "客服ID")
    private Long csId;

    @Schema(description = "审核员ID")
    private Long auditorId;

    @Schema(description = "订单状态")
    private String status;

    @Schema(description = "创建时间起")
    private LocalDateTime startTime;

    @Schema(description = "创建时间止")
    private LocalDateTime endTime;
}
