package com.bbpms.order.controller;

import com.bbpms.common.result.R;
import com.bbpms.order.entity.Appointment;
import com.bbpms.order.service.AppointmentService;
import com.bbpms.order.vo.AppointmentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Appointment endpoints.
 */
@Tag(name = "appointment", description = "预约管理")
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @Operation(summary = "创建预约")
    @PostMapping
    @PreAuthorize("hasAuthority('order:create')")
    public R<AppointmentVO> create(@RequestBody AppointmentCreateReq req) {
        Appointment a = appointmentService.create(
                req.getOrderId(), req.getAppointmentTime(),
                req.getContactPhone(), req.getRemark());
        return R.ok(toVO(a));
    }

    @Operation(summary = "查询订单的预约")
    @GetMapping("/by-order/{orderId}")
    @PreAuthorize("hasAuthority('order:view')")
    public R<List<AppointmentVO>> byOrder(@PathVariable("orderId") Long orderId) {
        List<Appointment> list = appointmentService.findByOrderId(orderId);
        if (list == null || list.isEmpty()) {
            return R.ok(Collections.emptyList());
        }
        return R.ok(list.stream().map(this::toVO).toList());
    }

    @Operation(summary = "更新订单的预约")
    @PutMapping("/by-order/{orderId}")
    @PreAuthorize("hasAuthority('order:update')")
    public R<AppointmentVO> update(@PathVariable("orderId") Long orderId,
                                   @RequestBody AppointmentCreateReq req) {
        Appointment a = appointmentService.update(
                orderId, req.getAppointmentTime(),
                req.getContactPhone(), req.getRemark());
        return R.ok(toVO(a));
    }

    private AppointmentVO toVO(Appointment a) {
        if (a == null) return null;
        AppointmentVO vo = new AppointmentVO();
        BeanUtils.copyProperties(a, vo);
        return vo;
    }

    @Data
    public static class AppointmentCreateReq implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long orderId;
        private LocalDateTime appointmentTime;
        private String contactPhone;
        private String remark;
    }
}
