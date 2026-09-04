package com.bbpms.customerportal.controller;

import com.bbpms.common.result.PageResp;
import com.bbpms.common.result.R;
import com.bbpms.customerportal.dto.PortalDtos;
import com.bbpms.customerportal.entity.CustomerServiceTicket;
import com.bbpms.customerportal.entity.ServiceEvaluation;
import com.bbpms.customerportal.service.CustomerPortalService;
import com.bbpms.notify.entity.Message;
import com.bbpms.order.dto.OrderDetailVO;
import com.bbpms.order.entity.Appointment;
import com.bbpms.resource.dto.ResourceCheckReq;
import com.bbpms.resource.vo.ResourceCheckResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "customer-portal", description = "客户自助端")
@RestController
@RequestMapping("/api/customer-portal")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerPortalController {
    private final CustomerPortalService service;

    @GetMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('customer-portal:profile:view')")
    public R<PortalDtos.ProfileVO> profile() { return R.ok(service.getOwnProfile()); }

    @PostMapping("/password")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('customer-portal:password:update')")
    public R<Void> changePassword(@Valid @RequestBody PortalDtos.PasswordChangeReq req) {
        service.changeOwnPassword(req); return R.ok();
    }

    @GetMapping("/packages")
    public R<List<PortalDtos.PackageVO>> packages() { return R.ok(service.listPackages()); }

    @PostMapping("/resources/check")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('customer-portal:order:create')")
    public R<ResourceCheckResp> checkResource(@Valid @RequestBody ResourceCheckReq req) {
        return R.ok(service.checkResource(req));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('customer-portal:order:view')")
    public R<PageResp<PortalDtos.CustomerOrderSummaryVO>> orders(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String status) {
        return R.ok(service.pageOwnOrders(pageNum, pageSize, status));
    }

    @GetMapping("/orders/{id}")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('customer-portal:order:view')")
    public R<OrderDetailVO> order(@PathVariable Long id) { return R.ok(service.getOwnOrder(id)); }

    @GetMapping("/orders/{id}/timeline")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('customer-portal:order:view')")
    public R<?> timeline(@PathVariable Long id) { return R.ok(service.getOwnOrder(id).getTimeline()); }

    @PostMapping("/orders")
    @Operation(summary = "客户自助报装")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('customer-portal:order:create')")
    public R<Long> createOrder(@Valid @RequestBody PortalDtos.CustomerOrderCreateReq req) {
        return R.ok(service.createOwnOrder(req));
    }

    @PutMapping("/orders/{id}/resubmit")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('customer-portal:order:create')")
    public R<Void> resubmitOrder(@PathVariable Long id,
                                 @Valid @RequestBody PortalDtos.CustomerOrderResubmitReq req) {
        service.resubmitOwnOrder(id, req); return R.ok();
    }

    @PutMapping("/orders/{id}/appointment")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('customer-portal:appointment:update')")
    public R<Appointment> reschedule(@PathVariable Long id,
                                     @Valid @RequestBody PortalDtos.AppointmentChangeReq req) {
        return R.ok(service.rescheduleOwnAppointment(id, req));
    }

    @PostMapping("/tickets")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('customer-portal:ticket:create')")
    public R<Long> createTicket(@Valid @RequestBody PortalDtos.TicketCreateReq req) {
        return R.ok(service.createTicket(req));
    }

    @GetMapping("/tickets")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('customer-portal:ticket:view')")
    public R<PageResp<CustomerServiceTicket>> tickets(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String type) {
        return R.ok(service.pageOwnTickets(pageNum, pageSize, type));
    }

    @GetMapping("/tickets/{id}")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('customer-portal:ticket:view')")
    public R<CustomerServiceTicket> ticket(@PathVariable Long id) { return R.ok(service.getOwnTicket(id)); }

    @PostMapping("/tickets/{id}/confirm")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('customer-portal:ticket:view')")
    public R<Void> confirmTicket(@PathVariable Long id) { service.confirmTicketResolved(id); return R.ok(); }

    @PostMapping("/orders/{id}/evaluation")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('customer-portal:evaluation:create')")
    public R<Long> evaluate(@PathVariable Long id,
                            @Valid @RequestBody PortalDtos.EvaluationCreateReq req) {
        return R.ok(service.evaluateOrder(id, req));
    }

    @GetMapping("/orders/{id}/evaluation")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('customer-portal:order:view')")
    public R<ServiceEvaluation> evaluation(@PathVariable Long id) {
        return R.ok(service.getOwnEvaluation(id));
    }

    @PostMapping("/profile/change-requests")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('customer-portal:profile:update')")
    public R<Long> requestProfileChange(@Valid @RequestBody PortalDtos.ProfileChangeReq req) {
        return R.ok(service.requestProfileChange(req));
    }

    @GetMapping("/profile/change-requests")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('customer-portal:profile:view')")
    public R<List<PortalDtos.ProfileChangeVO>> profileChanges() {
        return R.ok(service.listOwnProfileChanges());
    }

    @GetMapping("/messages")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('customer-portal:message:view')")
    public R<PageResp<Message>> messages(@RequestParam(defaultValue = "1") long pageNum,
                                         @RequestParam(defaultValue = "20") long pageSize) {
        return R.ok(service.pageOwnMessages(pageNum, pageSize));
    }

    @PutMapping("/messages/{id}/read")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('customer-portal:message:view')")
    public R<Void> readMessage(@PathVariable Long id) { service.readOwnMessage(id); return R.ok(); }
}
