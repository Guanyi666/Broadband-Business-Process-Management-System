package com.bbpms.order.controller;

import com.bbpms.common.annotation.OperationLog;
import com.bbpms.common.result.PageResp;
import com.bbpms.common.result.R;
import com.bbpms.common.util.SecurityUtils;
import com.bbpms.order.dto.AppointmentUpsertReq;
import com.bbpms.order.dto.OrderAuditReq;
import com.bbpms.order.dto.OrderCancelReq;
import com.bbpms.order.dto.OrderCreateReq;
import com.bbpms.order.dto.OrderDetailVO;
import com.bbpms.order.dto.OrderQueryReq;
import com.bbpms.order.dto.OrderTimelineVO;
import com.bbpms.order.entity.Appointment;
import com.bbpms.order.entity.BroadbandOrder;
import com.bbpms.order.service.AppointmentService;
import com.bbpms.order.service.OrderService;
import com.bbpms.order.vo.AppointmentVO;
import com.bbpms.order.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Order REST API. Authentication is enforced by the gateway and surfaced
 * via {@link SecurityUtils}. Write endpoints use {@link PreAuthorize} based
 * on {@code order:*} permission codes; {@link OperationLog} hooks the
 * operation-log listener (also a Spring event).
 */
@Slf4j
@Tag(name = "order", description = "订单管理")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final AppointmentService appointmentService;

    @Operation(summary = "创建订单")
    @PostMapping
    @PreAuthorize("hasAuthority('order:create')")
    @OperationLog(value = "创建订单", module = "订单")
    public R<Long> create(@Valid @RequestBody OrderCreateReq req) {
        Long csUserId = SecurityUtils.getCurrentUserId();
        log.info("Creating order package={} customer={} by cs={}",
                req.getPackageCode(), req.getCustomerId(), csUserId);
        return R.ok(orderService.create(req, csUserId));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('order:view') and !hasRole('CUSTOMER')")
    public R<OrderDetailVO> detail(@PathVariable("id") Long id) {
        return R.ok(orderService.getDetail(id));
    }

    @Operation(summary = "分页查询订单")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('order:view') and !hasRole('CUSTOMER')")
    public R<PageResp<OrderVO>> page(OrderQueryReq req) {
        return R.ok(orderService.page(req));
    }

    @Operation(summary = "按订单号查询")
    @GetMapping("/by-no/{orderNo}")
    @PreAuthorize("hasAuthority('order:view') and !hasRole('CUSTOMER')")
    public R<BroadbandOrder> byNo(@PathVariable("orderNo") String orderNo) {
        return R.ok(orderService.findByOrderNo(orderNo));
    }

    @Operation(summary = "审核订单")
    @PostMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('order:audit')")
    @OperationLog(value = "审核订单", module = "订单")
    public R<Void> audit(@PathVariable("id") Long id,
                         @Valid @RequestBody OrderAuditReq req) {
        Long auditorUserId = SecurityUtils.getCurrentUserId();
        orderService.audit(id, req, auditorUserId);
        return R.ok();
    }

    @Operation(summary = "重新提交被驳回的订单")
    @PostMapping("/{id}/resubmit")
    @PreAuthorize("hasAuthority('order:create')")
    @OperationLog(value = "重新提交订单", module = "订单")
    public R<Void> resubmit(@PathVariable("id") Long id) {
        orderService.resubmit(id, SecurityUtils.getCurrentUserId());
        return R.ok();
    }

    @Operation(summary = "取消订单")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('order:cancel')")
    @OperationLog(value = "取消订单", module = "订单")
    public R<Void> cancel(@PathVariable("id") Long id,
                          @Valid @RequestBody OrderCancelReq req) {
        orderService.cancel(id, req, SecurityUtils.getCurrentUserId());
        return R.ok();
    }

    @Operation(summary = "更新预约信息")
    @PutMapping("/{id}/appointment")
    @PreAuthorize("hasAuthority('order:update')")
    public R<AppointmentVO> upsertAppointment(@PathVariable("id") Long id,
                                              @Valid @RequestBody AppointmentUpsertReq req) {
        Appointment a = appointmentService.update(id,
                req.getAppointmentTime(), req.getContactPhone(), req.getRemark());
        return R.ok(toVO(a));
    }

    @Operation(summary = "订单时间线")
    @GetMapping("/{id}/timeline")
    @PreAuthorize("hasAuthority('order:view') and !hasRole('CUSTOMER')")
    public R<List<OrderTimelineVO>> timeline(@PathVariable("id") Long id) {
        return R.ok(orderService.getDetail(id).getTimeline());
    }

    private AppointmentVO toVO(Appointment a) {
        if (a == null) return null;
        AppointmentVO vo = new AppointmentVO();
        BeanUtils.copyProperties(a, vo);
        return vo;
    }
}
