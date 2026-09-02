package com.bbpms.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Scheduled appointment for an order — separated from {@link BroadbandOrder}
 * so the order's audit history is not polluted by date slips.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("appointment")
public class Appointment extends BaseDO {

    private Long orderId;

    private LocalDateTime appointmentTime;

    /** Free-form phone the installer should call. */
    private String contactPhone;

    private String remark;

    /** 1=customer confirmed, 0=pending. */
    private Integer confirmed;
}
