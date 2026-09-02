package com.bbpms.order.dto;

import com.bbpms.order.vo.AppointmentVO;
import com.bbpms.order.vo.CustomerVO;
import com.bbpms.order.vo.OrderVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Detail composite returned by {@code GET /api/orders/{id}}: order +
 * customer (masked or unmasked) + appointment + timeline.
 */
@Data
@Schema(description = "订单详情聚合")
public class OrderDetailVO {

    @Schema(description = "订单主体")
    private OrderVO order;

    @Schema(description = "客户信息")
    private CustomerVO customer;

    @Schema(description = "预约信息")
    private AppointmentVO appointment;

    @Schema(description = "完整时间线")
    private List<OrderTimelineVO> timeline;
}
