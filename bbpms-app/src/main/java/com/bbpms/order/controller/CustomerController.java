package com.bbpms.order.controller;

import com.bbpms.common.annotation.OperationLog;
import com.bbpms.common.entity.BaseDTO;
import com.bbpms.common.result.PageResp;
import com.bbpms.common.result.R;
import com.bbpms.common.util.SecurityUtils;
import com.bbpms.order.entity.Customer;
import com.bbpms.order.service.CustomerService;
import com.bbpms.order.vo.CustomerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Customer endpoints.
 *
 * <p>{@code GET /{id}} always returns masked PII; only
 * {@code GET /{id}/unmasked} (gated by the super-admin-only permission
 * {@code customer:view-sensitive}) returns plaintext.</p>
 */
@Slf4j
@Tag(name = "customer", description = "客户管理")
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "新增/幂等新建客户")
    @PostMapping
    @PreAuthorize("hasAuthority('customer:create')")
    @OperationLog(value = "创建客户", module = "客户")
    public R<Long> create(@RequestBody CustomerUpsertReq req) {
        Customer c = new Customer();
        c.setName(req.getName());
        c.setPhone(req.getPhone());
        c.setIdCardNo(req.getIdCardNo());
        c.setAddress(req.getAddress());
        c.setProvince(req.getProvince());
        c.setCity(req.getCity());
        c.setDistrict(req.getDistrict());
        return R.ok(customerService.upsert(c));
    }

    @Operation(summary = "获取客户（默认脱敏）")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('customer:view')")
    public R<CustomerVO> getById(@PathVariable("id") Long id) {
        return R.ok(customerService.getById(id, true));
    }

    @Operation(summary = "获取客户（明文，仅超级管理员）")
    @GetMapping("/{id}/unmasked")
    @PreAuthorize("hasAuthority('customer:view-sensitive')")
    @OperationLog(value = "查看客户敏感信息", module = "客户")
    public R<CustomerVO> getUnmaskedById(@PathVariable("id") Long id) {
        log.warn("Unmasked customer fetch user={} customer={}",
                SecurityUtils.getCurrentUserId(), id);
        return R.ok(customerService.getById(id, false));
    }

    @Operation(summary = "分页查询客户")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('customer:view')")
    public R<PageResp<CustomerVO>> page(CustomerPageReq req) {
        // List pagination exposes only masked PII via {@code customer:view}.
        // For sensitive listing use the per-id unmasked endpoint above.
        long pageNum = req.getPageNum() == null ? 1 : req.getPageNum();
        long pageSize = req.getPageSize() == null ? 20 : req.getPageSize();
        return R.ok(customerService.page((int) pageNum, (int) pageSize));
    }

    @Operation(summary = "模糊搜索客户（名称/手机号）")
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('customer:view')")
    public R<List<CustomerVO>> search(@RequestParam String keyword,
                                      @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return R.ok(customerService.search(keyword, limit));
    }

    @Operation(summary = "按手机号查询")
    @GetMapping("/by-phone/{phone}")
    @PreAuthorize("hasAuthority('customer:view')")
    public R<Long> findByPhone(@PathVariable("phone") String phone) {
        Customer c = customerService.findByPhone(phone);
        return R.ok(c == null ? null : c.getId());
    }

    @Data
    public static class CustomerUpsertReq implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String name;
        private String phone;
        private String idCardNo;
        private String address;
        private String province;
        private String city;
        private String district;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class CustomerPageReq extends BaseDTO {
    }
}
