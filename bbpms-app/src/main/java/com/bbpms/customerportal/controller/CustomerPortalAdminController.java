package com.bbpms.customerportal.controller;

import com.bbpms.common.result.PageResp;
import com.bbpms.common.result.R;
import com.bbpms.customerportal.dto.PortalDtos;
import com.bbpms.customerportal.entity.CustomerServiceTicket;
import com.bbpms.customerportal.service.CustomerPortalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer-portal/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('customer-portal:admin')")
public class CustomerPortalAdminController {
    private final CustomerPortalService service;

    @GetMapping("/customers/{customerId}/account")
    public R<PortalDtos.AccountVO> account(@PathVariable Long customerId) {
        return R.ok(service.getAccountByCustomer(customerId));
    }

    @PostMapping("/accounts")
    public R<PortalDtos.AccountVO> openAccount(@Valid @RequestBody PortalDtos.AccountCreateReq req) {
        return R.ok(service.openAccount(req));
    }

    @PutMapping("/customers/{customerId}/account/status")
    public R<PortalDtos.AccountVO> accountStatus(@PathVariable Long customerId,
                                                 @Valid @RequestBody PortalDtos.AccountStatusReq req) {
        return R.ok(service.setAccountStatus(customerId, req.getStatus()));
    }

    @PostMapping("/customers/{customerId}/account/password")
    public R<Void> resetPassword(@PathVariable Long customerId,
                                 @Valid @RequestBody PortalDtos.PasswordResetReq req) {
        service.resetPassword(customerId, req.getNewPassword()); return R.ok();
    }

    @GetMapping("/orders")
    public R<PageResp<PortalDtos.CustomerOrderSummaryVO>> customerOrders(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String status) {
        return R.ok(service.pageCustomerSubmittedOrders(pageNum, pageSize, status));
    }

    @PostMapping("/orders/{id}/review")
    public R<Void> reviewOrder(@PathVariable Long id,
                               @Valid @RequestBody PortalDtos.OrderReviewReq req) {
        service.reviewCustomerOrder(id, req); return R.ok();
    }

    @GetMapping("/tickets")
    public R<PageResp<CustomerServiceTicket>> tickets(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        return R.ok(service.pageTicketsForAdmin(pageNum, pageSize, type, status));
    }

    @PutMapping("/tickets/{id}")
    public R<Void> handleTicket(@PathVariable Long id,
                                @Valid @RequestBody PortalDtos.TicketHandleReq req) {
        service.handleTicket(id, req); return R.ok();
    }

    @GetMapping("/profile-changes")
    public R<PageResp<PortalDtos.ProfileChangeVO>> profileChanges(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String status) {
        return R.ok(service.pageProfileChangesForAdmin(pageNum, pageSize, status));
    }

    @PostMapping("/profile-changes/{id}/review")
    public R<Void> reviewProfileChange(@PathVariable Long id,
                                       @Valid @RequestBody PortalDtos.ProfileReviewReq req) {
        service.reviewProfileChange(id, req); return R.ok();
    }
}
