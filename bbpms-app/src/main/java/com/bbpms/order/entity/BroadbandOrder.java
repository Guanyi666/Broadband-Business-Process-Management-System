package com.bbpms.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import com.bbpms.common.enums.OrderStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Broadband install order aggregate root.
 *
 * <p>Persists {@link OrderStatus} as its {@code String} code (via MP's
 * default enum handler); transitions are validated by
 * {@link com.bbpms.common.statemachine.OrderStateMachine} before update.</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("broadband_order")
public class BroadbandOrder extends BaseDO {

    /** Business order number (UK), generated as "BB" + snowflake-id. */
    private String orderNo;

    private Long customerId;

    private String packageCode;
    private String packageName;

    private String installAddress;

    private LocalDateTime expectedInstallDate;

    /** String code of {@link OrderStatus} — kept as String for backward compat. */
    private String status;

    /** Customer-service user id at the time of creation. */
    private Long csId;

    private Long auditorId;
    private LocalDateTime auditTime;
    private String auditRemark;

    private LocalDateTime dispatchTime;
    private LocalDateTime completedTime;
    private LocalDateTime cancelledTime;
    private String cancelReason;
}
