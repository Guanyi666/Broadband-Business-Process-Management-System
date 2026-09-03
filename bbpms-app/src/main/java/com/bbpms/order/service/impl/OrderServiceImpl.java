package com.bbpms.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bbpms.common.annotation.OperationLog;
import com.bbpms.common.dto.OrderSummaryDTO;
import com.bbpms.common.enums.OrderEvent;
import com.bbpms.common.enums.OrderStatus;
import com.bbpms.common.enums.ResultCode;
import com.bbpms.common.event.BbpmsEvents;
import com.bbpms.common.exception.BizException;
import com.bbpms.common.result.PageResp;
import com.bbpms.common.util.SecurityUtils;
import com.bbpms.common.statemachine.OrderStateMachine;
import com.bbpms.common.util.CryptoUtils;
import com.bbpms.common.util.SnowflakeIdGenerator;
import com.bbpms.order.config.OrderProperties;
import com.bbpms.order.dto.OrderAuditReq;
import com.bbpms.order.dto.OrderCancelReq;
import com.bbpms.order.dto.OrderCreateReq;
import com.bbpms.order.dto.OrderDetailVO;
import com.bbpms.order.dto.OrderQueryReq;
import com.bbpms.order.entity.Appointment;
import com.bbpms.order.entity.BroadbandOrder;
import com.bbpms.order.entity.Customer;
import com.bbpms.order.entity.OrderAuditLog;
import com.bbpms.order.mapper.AppointmentMapper;
import com.bbpms.order.mapper.BroadbandOrderMapper;
import com.bbpms.order.mapper.CustomerMapper;
import com.bbpms.order.mapper.OrderAuditLogMapper;
import com.bbpms.order.service.CustomerService;
import com.bbpms.order.service.OrderService;
import com.bbpms.order.service.OrderTimelineService;
import com.bbpms.order.vo.AppointmentVO;
import com.bbpms.order.vo.CustomerVO;
import com.bbpms.order.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Order lifecycle service implementation (monolith edition).
 *
 * <p>Every multi-table write runs in a single
 * {@link Transactional @Transactional} boundary. Outbound effects are
 * published via {@link ApplicationEventPublisher} — listeners that need
 * "post-commit only" semantics use {@code @TransactionalEventListener}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    /** Prefix for all business order numbers. */
    public static final String ORDER_NO_PREFIX = "BB";

    private final BroadbandOrderMapper orderMapper;
    private final OrderAuditLogMapper auditLogMapper;
    private final AppointmentMapper appointmentMapper;
    private final CustomerMapper customerMapper;
    private final CustomerService customerService;
    private final OrderProperties orderProperties;
    private final OrderStateMachine orderStateMachine;
    private final ApplicationEventPublisher publisher;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    /** Lazy to break the bidirectional dependency with OrderTimelineService. */
    private final @Lazy OrderTimelineService orderTimelineService;

    /* ===================== CREATE ===================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(value = "创建宽带订单", module = "订单")
    public Long create(OrderCreateReq req, Long csUserId) {
        if (req == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "请求不能为空");
        }
        if (req.getPackageCode() == null || req.getPackageCode().isBlank()) {
            throw new BizException(ResultCode.BAD_REQUEST, "套餐编码不能为空");
        }
        if (req.getInstallAddress() == null || req.getInstallAddress().isBlank()) {
            throw new BizException(ResultCode.BAD_REQUEST, "安装地址不能为空");
        }
        if (req.getCustomerId() == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "customerId 必填");
        }

        // Validate customer.
        Customer existing = customerMapper.selectById(req.getCustomerId());
        if (existing == null) {
            throw new BizException(ResultCode.CUSTOMER_NOT_FOUND);
        }

        // Generate orderNo.
        String orderNo = ORDER_NO_PREFIX + snowflakeIdGenerator.nextId();

        // Insert order.
        BroadbandOrder order = new BroadbandOrder();
        order.setOrderNo(orderNo);
        order.setCustomerId(existing.getId());
        order.setPackageCode(req.getPackageCode());
        order.setPackageName(req.getPackageName() != null && !req.getPackageName().isBlank()
                ? req.getPackageName() : req.getPackageCode());
        order.setInstallAddress(req.getInstallAddress());
        order.setExpectedInstallDate(req.getExpectedInstallDate());
        order.setStatus(OrderStatus.CREATED.name());
        order.setCsId(csUserId);
        order.setCreateBy(csUserId);
        order.setUpdateBy(csUserId);
        orderMapper.insert(order);

        // Optional appointment.
        if (req.getAppointmentTime() != null) {
            Appointment appt = new Appointment();
            appt.setOrderId(order.getId());
            appt.setAppointmentTime(req.getAppointmentTime());
            appt.setContactPhone(req.getContactPhone());
            appt.setRemark(req.getRemark());
            appt.setConfirmed(0);
            appt.setCreateBy(csUserId);
            appt.setUpdateBy(csUserId);
            appointmentMapper.insert(appt);
        }

        // Audit log row — null -> CREATED.
        appendAuditLog(order.getId(), csUserId,
                null, OrderStatus.CREATED.name(), "订单创建");

        // Publish — same-process event replaces RabbitMQ.
        publisher.publishEvent(new BbpmsEvents.OrderCreatedEvent(
                order.getId(), order.getOrderNo(), order.getCustomerId(),
                order.getPackageCode(), order.getCsId()));

        log.info("Created order {} (customer={}, cs={})",
                order.getOrderNo(), order.getCustomerId(), csUserId);
        return order.getId();
    }

    /* ===================== AUDIT ===================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(value = "审核订单", module = "订单")
    public void audit(Long orderId, OrderAuditReq req, Long auditorId) {
        if (orderId == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "orderId 不能为空");
        }
        BroadbandOrder order = mustLoad(orderId);
        OrderStatus current = OrderStatus.fromCode(order.getStatus());
        if (current == null) {
            throw new BizException(ResultCode.ORDER_STATUS_INVALID, "订单状态未知");
        }
        boolean pass = Boolean.TRUE.equals(req.getPass());
        OrderEvent event = pass ? OrderEvent.AUDIT_PASS : OrderEvent.AUDIT_REJECT;

        // Validate transition via the common state machine.
        OrderStatus next = orderStateMachine.next(current, event);

        // Apply update + audit fields.
        OrderStatus before = current;
        order.setStatus(next.name());
        order.setAuditorId(auditorId);
        order.setAuditTime(LocalDateTime.now());
        order.setAuditRemark(req.getRemark());
        if (next == OrderStatus.CANCELLED) {
            order.setCancelledTime(LocalDateTime.now());
            order.setCancelReason(req.getRemark());
        }
        order.setUpdateBy(auditorId);
        orderMapper.updateById(order);

        appendAuditLog(order.getId(), auditorId,
                before.name(), next.name(), req.getRemark());

        // Optional appointment upsert on pass.
        if (pass && req.getAppointmentTime() != null) {
            upsertAppointmentOnAudit(order.getId(), req.getAppointmentTime(),
                    req.getRemark(), auditorId);
        }

        // Publish event for downstream listeners (workorder auto-dispatch etc.).
        if (pass) {
            publisher.publishEvent(new BbpmsEvents.OrderAuditedEvent(
                    order.getId(), order.getOrderNo(), auditorId, order.getAuditTime()));
        } else if (next == OrderStatus.CANCELLED) {
            // Only a real cancellation (or cancel-after-reject) cancels work orders.
            // A plain REJECT keeps the order resubmittable, so no cancel event is fired.
            publisher.publishEvent(new BbpmsEvents.OrderCancelledEvent(
                    order.getId(), auditorId, req.getRemark()));
        }

        log.info("Audit on order {} -> {} by {}", order.getOrderNo(), next, auditorId);
    }

    /* ===================== RESUBMIT ===================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(value = "重新提交订单", module = "订单")
    public void resubmit(Long orderId, Long operatorId) {
        if (orderId == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "orderId 不能为空");
        }
        BroadbandOrder order = mustLoad(orderId);
        OrderStatus current = OrderStatus.fromCode(order.getStatus());
        if (current == null) {
            throw new BizException(ResultCode.ORDER_STATUS_INVALID, "订单状态未知");
        }
        OrderStatus next = orderStateMachine.next(current, OrderEvent.RESUBMIT);

        String before = current.name();
        order.setStatus(next.name());
        // A fresh audit round should not carry the previous rejection remark.
        order.setAuditRemark(null);
        order.setUpdateBy(operatorId);
        orderMapper.updateById(order);

        appendAuditLog(order.getId(), operatorId,
                before, next.name(), "驳回后重新提交，重新进入审核队列");

        log.info("Order {} resubmitted {} -> {} by {}", order.getOrderNo(), before, next, operatorId);
    }

    /* ===================== CANCEL ===================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(value = "取消订单", module = "订单")
    public void cancel(Long orderId, OrderCancelReq req, Long operatorId) {
        if (orderId == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "orderId 不能为空");
        }
        BroadbandOrder order = mustLoad(orderId);
        OrderStatus current = OrderStatus.fromCode(order.getStatus());
        if (current == null) {
            throw new BizException(ResultCode.ORDER_STATUS_INVALID, "订单状态未知");
        }
        OrderStatus next = orderStateMachine.next(current, OrderEvent.CANCEL);

        String before = current.name();
        order.setStatus(next.name());
        order.setCancelledTime(LocalDateTime.now());
        order.setCancelReason(req == null ? null : req.getReason());
        order.setUpdateBy(operatorId);
        orderMapper.updateById(order);

        appendAuditLog(order.getId(), operatorId,
                before, next.name(),
                req == null ? null : req.getReason());

        publisher.publishEvent(new BbpmsEvents.OrderCancelledEvent(
                order.getId(), operatorId,
                req == null ? null : req.getReason()));
    }

    /* ===================== DETAIL ===================== */

    @Override
    public OrderDetailVO getDetail(Long orderId) {
        BroadbandOrder order = mustLoad(orderId);
        OrderDetailVO vo = new OrderDetailVO();
        vo.setOrder(toOrderVO(order));

        Customer c = customerMapper.selectById(order.getCustomerId());
        // Default masked — the controller decides whether to unmask based on the
        // caller's permission.
        vo.setCustomer(toCustomerVO(c, false));

        List<Appointment> appts = appointmentMapper.selectByOrderId(orderId);
        if (appts != null && !appts.isEmpty()) {
            vo.setAppointment(toAppointmentVO(appts.get(0)));
        }
        vo.setTimeline(orderTimelineService.getTimeline(orderId));
        return vo;
    }

    /* ===================== PAGE ===================== */

    @Override
    public PageResp<OrderVO> page(OrderQueryReq req) {
        Page<BroadbandOrder> page = new Page<>(
                req.getPageNum() == null ? 1L : req.getPageNum(),
                req.getPageSize() == null ? 20L : req.getPageSize());
        LambdaQueryWrapper<BroadbandOrder> wrapper = new LambdaQueryWrapper<>();
        if (req.getOrderNo() != null)    wrapper.eq(BroadbandOrder::getOrderNo, req.getOrderNo());
        if (req.getCustomerId() != null) wrapper.eq(BroadbandOrder::getCustomerId, req.getCustomerId());
        if (req.getCsId() != null)       wrapper.eq(BroadbandOrder::getCsId, req.getCsId());
        if (req.getStatus() != null)     wrapper.eq(BroadbandOrder::getStatus, req.getStatus());
        if (req.getStartTime() != null)  wrapper.ge(BroadbandOrder::getCreateTime, req.getStartTime());
        if (req.getEndTime() != null)    wrapper.le(BroadbandOrder::getCreateTime, req.getEndTime());
        if (req.getOrderBy() != null && !req.getOrderBy().isBlank()) {
            // SA-P2-006: orderBy is interpolated raw into SQL — restrict to a
            // whitelist of real broadband_order columns + asc/desc direction.
            wrapper.last("ORDER BY " + sanitizeOrderBy(req.getOrderBy()));
        } else {
            wrapper.orderByDesc(BroadbandOrder::getCreateTime);
        }
        IPage<BroadbandOrder> result = orderMapper.selectPageWithScope(page, wrapper);
        Page<BroadbandOrder> paged = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        paged.setRecords(result.getRecords());
        IPage<OrderVO> voPage = paged.convert(this::toOrderVO);
        return com.bbpms.common.result.PageResp.of(voPage);
    }

    /* ===================== orderBy whitelist (SA-P2-006) ===================== */

    /** Real broadband_order columns that callers may sort by. */
    private static final Set<String> ORDER_BY_COLUMNS = Set.of(
            "create_time", "update_time", "order_no", "status",
            "expected_install_date", "appointment_time");
    private static final Pattern ORDER_BY_PATTERN =
            Pattern.compile("^([a-zA-Z_]+)\\s+(asc|desc)$", Pattern.CASE_INSENSITIVE);

    private String sanitizeOrderBy(String raw) {
        String input = raw.trim();
        Matcher m = ORDER_BY_PATTERN.matcher(input);
        if (m.matches()) {
            String col = m.group(1).toLowerCase(Locale.ROOT);
            if (ORDER_BY_COLUMNS.contains(col)) {
                return col + " " + m.group(2).toLowerCase(Locale.ROOT);
            }
        }
        throw new BizException(ResultCode.BAD_REQUEST, "非法的排序字段: " + raw);
    }

    /* ===================== findByOrderId ===================== */

    @Override
    public BroadbandOrder findByOrderId(Long orderId) {
        BroadbandOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    /* ===================== findByOrderNo ===================== */

    @Override
    public BroadbandOrder findByOrderNo(String orderNo) {
        BroadbandOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    /* ===================== updateStatus (internal API) ===================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long orderId, OrderStatus status, Long operatorId) {
        if (orderId == null || status == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "orderId 和 status 必填");
        }
        BroadbandOrder order = mustLoad(orderId);
        String before = order.getStatus();

        order.setStatus(status.name());
        if (status == OrderStatus.DISPATCHED) {
            order.setDispatchTime(LocalDateTime.now());
        } else if (status == OrderStatus.FINISHED) {
            order.setCompletedTime(LocalDateTime.now());
        } else if (status == OrderStatus.CANCELLED) {
            order.setCancelledTime(LocalDateTime.now());
        }
        order.setUpdateBy(operatorId != null ? operatorId : SecurityUtils.getCurrentUserId());
        orderMapper.updateById(order);

        appendAuditLog(order.getId(), order.getUpdateBy(),
                before, status.name(), "external update by module caller");
    }

    @Override
    public OrderSummaryDTO snapshot(Long orderId) {
        BroadbandOrder order = orderMapper.selectById(orderId);
        if (order == null) return null;
        OrderSummaryDTO.OrderSummaryDTOBuilder b = OrderSummaryDTO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .customerId(order.getCustomerId())
                .installAddress(order.getInstallAddress())
                .packageCode(order.getPackageCode())
                .packageName(order.getPackageName())
                .status(order.getStatus());
        // Latest appointment contact phone.
        Appointment appt = appointmentMapper.selectOne(
                new LambdaQueryWrapper<Appointment>().eq(Appointment::getOrderId, orderId)
                        .orderByDesc(Appointment::getAppointmentTime).last("LIMIT 1"));
        if (appt != null) {
            b.contactPhone(appt.getContactPhone());
        }
        return b.build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markFinished(Long orderId, Long operatorId) {
        if (orderId == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "orderId 不能为空");
        }
        BroadbandOrder order = mustLoad(orderId);
        OrderStatus current = OrderStatus.fromCode(order.getStatus());
        if (current == null) {
            throw new BizException(ResultCode.ORDER_STATUS_INVALID, "订单状态未知");
        }
        // Idempotent no-op when the order is already FINISHED: the install
        // flow flips the parent order twice (work-order complete mirrors the
        // order via updateStatus, then InstallServiceImpl.complete calls
        // markFinished again). Throwing here marks the outer transaction
        // rollback-only even though the caller catches — which surfaces as
        // UnexpectedRollbackException on commit.
        if (current == OrderStatus.FINISHED) {
            return;
        }
        // Only the two states the install flow can transition from are valid.
        if (current != OrderStatus.DISPATCHED && current != OrderStatus.INSTALLING) {
            throw new BizException(ResultCode.ORDER_STATUS_INVALID,
                    "当前状态 " + current + " 不允许转为 FINISHED");
        }
        String before = order.getStatus();
        order.setStatus(OrderStatus.FINISHED.name());
        order.setCompletedTime(LocalDateTime.now());
        order.setUpdateBy(operatorId != null ? operatorId : SecurityUtils.getCurrentUserId());
        int rows = orderMapper.updateById(order);
        if (rows == 0) {
            throw new BizException(ResultCode.VERSION_CONFLICT, "订单版本冲突，请刷新后重试");
        }
        appendAuditLog(orderId, order.getUpdateBy(),
                before, OrderStatus.FINISHED.name(), "装机完成，进入待确认");
        log.info("Order {} marked FINISHED (was {}) by {}", orderId, before, order.getUpdateBy());
    }

    /* ===================== helpers ===================== */

    private BroadbandOrder mustLoad(Long orderId) {
        BroadbandOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private void appendAuditLog(Long orderId, Long operatorId, String from, String to, String remark) {
        OrderAuditLog log = new OrderAuditLog();
        log.setOrderId(orderId);
        log.setAuditorId(operatorId);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setRemark(remark);
        log.setCreateBy(operatorId);
        log.setUpdateBy(operatorId);
        auditLogMapper.insert(log);
    }

    private void upsertAppointmentOnAudit(Long orderId, LocalDateTime time, String remark, Long operatorId) {
        Appointment latest = appointmentMapper.selectOne(
                new LambdaQueryWrapper<Appointment>().eq(Appointment::getOrderId, orderId)
                        .orderByDesc(Appointment::getAppointmentTime).last("LIMIT 1"));
        if (latest == null) {
            Appointment a = new Appointment();
            a.setOrderId(orderId);
            a.setAppointmentTime(time);
            a.setRemark(remark);
            a.setConfirmed(0);
            a.setCreateBy(operatorId);
            a.setUpdateBy(operatorId);
            appointmentMapper.insert(a);
        } else {
            latest.setAppointmentTime(time);
            latest.setRemark(remark);
            latest.setUpdateBy(operatorId);
            appointmentMapper.updateById(latest);
        }
    }

    /* ===================== VO conversion ===================== */

    public OrderVO toOrderVO(BroadbandOrder o) {
        if (o == null) return null;
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(o, vo);
        return vo;
    }

    public AppointmentVO toAppointmentVO(Appointment a) {
        if (a == null) return null;
        AppointmentVO vo = new AppointmentVO();
        BeanUtils.copyProperties(a, vo);
        return vo;
    }

    public CustomerVO toCustomerVO(Customer c, boolean unmasked) {
        if (c == null) return null;
        CustomerVO vo = new CustomerVO();
        BeanUtils.copyProperties(c, vo);
        vo.setMasked(!unmasked);
        try {
            String key = orderProperties.getSm4Key();
            if (c.getName() != null) {
                String plain = CryptoUtils.sm4Decrypt(c.getName(), key);
                vo.setName(unmasked ? plain : CryptoUtils.maskPhone(plain));
            }
            if (c.getPhone() != null) {
                String plain = CryptoUtils.sm4Decrypt(c.getPhone(), key);
                vo.setPhone(unmasked ? plain : CryptoUtils.maskPhone(plain));
            }
            if (c.getIdCardNo() != null) {
                String plain = CryptoUtils.sm4Decrypt(c.getIdCardNo(), key);
                vo.setIdCardNo(unmasked ? plain : CryptoUtils.maskIdCard(plain));
            }
        } catch (Exception ignored) {
            // Masking is best-effort.
        }
        return vo;
    }
}