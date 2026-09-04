package com.bbpms.customerportal.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bbpms.common.enums.OrderStatus;
import com.bbpms.common.enums.ResultCode;
import com.bbpms.common.event.BbpmsEvents;
import com.bbpms.common.exception.BizException;
import com.bbpms.common.result.PageResp;
import com.bbpms.common.util.CryptoUtils;
import com.bbpms.common.util.SecurityUtils;
import com.bbpms.common.util.SnowflakeIdGenerator;
import com.bbpms.customerportal.dto.PortalDtos;
import com.bbpms.customerportal.entity.*;
import com.bbpms.customerportal.mapper.*;
import com.bbpms.notify.entity.Message;
import com.bbpms.notify.mapper.MessageMapper;
import com.bbpms.order.config.OrderProperties;
import com.bbpms.order.dto.OrderDetailVO;
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
import com.bbpms.order.vo.CustomerVO;
import com.bbpms.resource.dto.ResourceCheckReq;
import com.bbpms.resource.service.ResourceCheckService;
import com.bbpms.resource.vo.ResourceCheckResp;
import com.bbpms.user.dto.PasswordChangeReq;
import com.bbpms.user.dto.UserCreateReq;
import com.bbpms.user.dto.UserUpdateReq;
import com.bbpms.user.entity.InstallerProfile;
import com.bbpms.user.entity.SysUser;
import com.bbpms.user.mapper.InstallerProfileMapper;
import com.bbpms.user.mapper.SysUserMapper;
import com.bbpms.user.service.SysUserService;
import com.bbpms.workorder.entity.WorkOrder;
import com.bbpms.workorder.mapper.WorkOrderMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Customer self-service application service. Every read starts from the authenticated binding. */
@Service
@RequiredArgsConstructor
public class CustomerPortalService {
    private static final Set<String> TICKET_TYPES = Set.of("REPAIR", "COMPLAINT");
    private static final Set<String> TICKET_STATUSES = Set.of(
            "SUBMITTED", "ACCEPTED", "PROCESSING", "WAIT_CONFIRM", "CLOSED", "REJECTED");

    private final CustomerUserBindingMapper bindingMapper;
    private final BroadbandPackageMapper packageMapper;
    private final CustomerServiceTicketMapper ticketMapper;
    private final ServiceEvaluationMapper evaluationMapper;
    private final CustomerProfileChangeMapper profileChangeMapper;
    private final AppointmentChangeLogMapper appointmentChangeLogMapper;
    private final BroadbandOrderMapper orderMapper;
    private final CustomerMapper customerMapper;
    private final AppointmentMapper appointmentMapper;
    private final OrderAuditLogMapper orderAuditLogMapper;
    private final WorkOrderMapper workOrderMapper;
    private final InstallerProfileMapper installerProfileMapper;
    private final MessageMapper messageMapper;
    private final SysUserMapper sysUserMapper;
    private final SysUserService userService;
    private final CustomerService customerService;
    private final OrderService orderService;
    private final ResourceCheckService resourceCheckService;
    private final OrderProperties orderProperties;
    private final SnowflakeIdGenerator snowflake;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher publisher;

    public CustomerUserBinding requireCurrentBinding() {
        Long userId = SecurityUtils.requireUserId();
        CustomerUserBinding binding = bindingMapper.selectOne(new LambdaQueryWrapper<CustomerUserBinding>()
                .eq(CustomerUserBinding::getUserId, userId)
                .eq(CustomerUserBinding::getStatus, 1)
                .last("LIMIT 1"));
        if (binding == null) {
            throw new BizException(ResultCode.FORBIDDEN, "当前账号未绑定客户资料或绑定已停用");
        }
        return binding;
    }

    private BroadbandOrder requireOwnedOrder(Long orderId) {
        if (orderId == null) throw new BizException(ResultCode.BAD_REQUEST, "orderId 必填");
        Long customerId = requireCurrentBinding().getCustomerId();
        BroadbandOrder order = orderMapper.selectById(orderId);
        if (order == null || !customerId.equals(order.getCustomerId())) {
            // Deliberately return not-found instead of leaking another customer's order existence.
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    /* -------------------- account administration -------------------- */

    public PortalDtos.AccountVO getAccountByCustomer(Long customerId) {
        CustomerUserBinding binding = bindingMapper.selectOne(new LambdaQueryWrapper<CustomerUserBinding>()
                .eq(CustomerUserBinding::getCustomerId, customerId).last("LIMIT 1"));
        return binding == null ? null : toAccountVO(binding);
    }

    @Transactional(rollbackFor = Exception.class)
    public PortalDtos.AccountVO openAccount(PortalDtos.AccountCreateReq req) {
        Customer customer = customerMapper.selectById(req.getCustomerId());
        if (customer == null) throw new BizException(ResultCode.CUSTOMER_NOT_FOUND);
        if (getAccountByCustomer(req.getCustomerId()) != null) {
            throw new BizException(ResultCode.BAD_REQUEST, "该客户已开通自助账号");
        }

        CustomerVO customerVO = customerService.getById(req.getCustomerId(), false);
        UserCreateReq userReq = new UserCreateReq();
        userReq.setUsername(req.getUsername().trim());
        userReq.setPassword(req.getPassword());
        userReq.setRealName(customerVO.getName());
        userReq.setUserType(6);
        userReq.setStatus(1);
        userReq.setRoleIds(List.of(6L));
        Long userId = userService.create(userReq);

        CustomerUserBinding binding = new CustomerUserBinding();
        binding.setUserId(userId);
        binding.setCustomerId(req.getCustomerId());
        binding.setStatus(1);
        binding.setBindTime(LocalDateTime.now());
        bindingMapper.insert(binding);
        return toAccountVO(binding);
    }

    @Transactional(rollbackFor = Exception.class)
    public PortalDtos.AccountVO setAccountStatus(Long customerId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ResultCode.BAD_REQUEST, "status 只能为 0 或 1");
        }
        CustomerUserBinding binding = requireBindingByCustomer(customerId);
        binding.setStatus(status);
        bindingMapper.updateById(binding);
        SysUser user = userService.getById(binding.getUserId());
        UserUpdateReq update = new UserUpdateReq();
        update.setId(user.getId());
        update.setRealName(user.getRealName());
        update.setPhone(user.getPhone());
        update.setEmail(user.getEmail());
        update.setDeptId(user.getDeptId());
        update.setStatus(status);
        userService.update(update);
        return toAccountVO(binding);
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long customerId, String newPassword) {
        CustomerUserBinding binding = requireBindingByCustomer(customerId);
        SysUser user = userService.getById(binding.getUserId());
        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeOwnPassword(PortalDtos.PasswordChangeReq req) {
        PasswordChangeReq change = new PasswordChangeReq();
        change.setOldPassword(req.getOldPassword());
        change.setNewPassword(req.getNewPassword());
        userService.changePassword(SecurityUtils.requireUserId(), change);
    }

    private CustomerUserBinding requireBindingByCustomer(Long customerId) {
        CustomerUserBinding binding = bindingMapper.selectOne(new LambdaQueryWrapper<CustomerUserBinding>()
                .eq(CustomerUserBinding::getCustomerId, customerId).last("LIMIT 1"));
        if (binding == null) throw new BizException(ResultCode.NOT_FOUND, "客户尚未开通自助账号");
        return binding;
    }

    private PortalDtos.AccountVO toAccountVO(CustomerUserBinding binding) {
        SysUser user = userService.getById(binding.getUserId());
        PortalDtos.AccountVO vo = new PortalDtos.AccountVO();
        vo.setCustomerId(binding.getCustomerId());
        vo.setUserId(binding.getUserId());
        vo.setStatus(binding.getStatus());
        vo.setBindTime(binding.getBindTime());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname() != null ? user.getNickname() : user.getRealName());
            vo.setLastLoginTime(user.getLastLoginAt());
        }
        return vo;
    }

    /* -------------------- profile and packages -------------------- */

    public PortalDtos.ProfileVO getOwnProfile() {
        CustomerUserBinding binding = requireCurrentBinding();
        PortalDtos.ProfileVO vo = new PortalDtos.ProfileVO();
        vo.setAccount(toAccountVO(binding));
        vo.setCustomer(customerService.getById(binding.getCustomerId(), false));
        return vo;
    }

    public List<PortalDtos.PackageVO> listPackages() {
        return packageMapper.selectList(new LambdaQueryWrapper<BroadbandPackage>()
                        .eq(BroadbandPackage::getStatus, 1)
                        .orderByAsc(BroadbandPackage::getSort))
                .stream().map(this::toPackageVO).toList();
    }

    private PortalDtos.PackageVO toPackageVO(BroadbandPackage p) {
        PortalDtos.PackageVO vo = new PortalDtos.PackageVO();
        vo.setId(p.getId());
        vo.setCode(p.getCode());
        vo.setName(p.getName());
        vo.setSpeedMbps(p.getSpeedMbps());
        vo.setMonthlyFee(p.getMonthlyFee());
        vo.setDescription(p.getDescription());
        return vo;
    }

    public ResourceCheckResp checkResource(ResourceCheckReq req) {
        requireCurrentBinding();
        return resourceCheckService.check(req);
    }

    /* -------------------- orders -------------------- */

    public PageResp<PortalDtos.CustomerOrderSummaryVO> pageOwnOrders(long pageNum, long pageSize, String status) {
        long safeSize = Math.min(Math.max(pageSize, 1), 100);
        Long customerId = requireCurrentBinding().getCustomerId();
        LambdaQueryWrapper<BroadbandOrder> q = new LambdaQueryWrapper<BroadbandOrder>()
                .eq(BroadbandOrder::getCustomerId, customerId)
                .orderByDesc(BroadbandOrder::getCreateTime);
        if (StringUtils.hasText(status)) q.eq(BroadbandOrder::getStatus, status);
        Page<BroadbandOrder> page = orderMapper.selectPage(new Page<>(Math.max(pageNum, 1), safeSize), q);
        PageResp<PortalDtos.CustomerOrderSummaryVO> resp = new PageResp<>();
        resp.setPageNum(page.getCurrent());
        resp.setPageSize(page.getSize());
        resp.setTotal(page.getTotal());
        resp.setPages(page.getPages());
        resp.setRecords(page.getRecords().stream().map(this::toOrderSummary).toList());
        return resp;
    }

    public OrderDetailVO getOwnOrder(Long orderId) {
        requireOwnedOrder(orderId);
        return orderService.getDetail(orderId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createOwnOrder(PortalDtos.CustomerOrderCreateReq req) {
        CustomerUserBinding binding = requireCurrentBinding();
        BroadbandPackage pkg = packageMapper.selectOne(new LambdaQueryWrapper<BroadbandPackage>()
                .eq(BroadbandPackage::getCode, req.getPackageCode())
                .eq(BroadbandPackage::getStatus, 1).last("LIMIT 1"));
        if (pkg == null) throw new BizException(ResultCode.PACKAGE_INVALID);

        ResourceCheckReq checkReq = new ResourceCheckReq();
        checkReq.setAddress(req.getInstallAddress());
        checkReq.setRoomNo(req.getRoomNo());
        ResourceCheckResp checked = resourceCheckService.check(checkReq);
        if (!"RESOURCE_OK".equals(checked.getStatus())) {
            throw new BizException(ResultCode.ADDRESS_NOT_COVERED, checked.getMessage());
        }

        Long userId = SecurityUtils.requireUserId();
        BroadbandOrder order = new BroadbandOrder();
        order.setOrderNo("BB" + snowflake.nextId());
        order.setCustomerId(binding.getCustomerId());
        order.setPackageCode(pkg.getCode());
        order.setPackageName(pkg.getName());
        order.setInstallAddress(req.getInstallAddress().trim());
        order.setExpectedInstallDate(req.getAppointmentTime());
        order.setStatus(OrderStatus.PENDING_CS_CONFIRM.name());
        order.setSource("CUSTOMER_SELF");
        order.setRoomId(checked.getRoomId());
        order.setResourceStatus(checked.getStatus());
        order.setCheckRemark(checked.getMessage());
        order.setCreateBy(userId);
        order.setUpdateBy(userId);
        orderMapper.insert(order);

        if (req.getAppointmentTime() != null) {
            Appointment appointment = new Appointment();
            appointment.setOrderId(order.getId());
            appointment.setAppointmentTime(req.getAppointmentTime());
            appointment.setContactPhone(req.getContactPhone());
            appointment.setRemark(req.getRemark());
            appointment.setConfirmed(0);
            appointment.setStatus("PENDING");
            appointment.setRescheduleCount(0);
            appointment.setCreateBy(userId);
            appointment.setUpdateBy(userId);
            appointmentMapper.insert(appointment);
        }
        appendOrderLog(order.getId(), null, OrderStatus.PENDING_CS_CONFIRM.name(), "客户自助提交报装", userId);
        publisher.publishEvent(new BbpmsEvents.OrderCreatedEvent(
                order.getId(), order.getOrderNo(), order.getCustomerId(), order.getPackageCode(), null));
        return order.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void resubmitOwnOrder(Long orderId, PortalDtos.CustomerOrderResubmitReq req) {
        BroadbandOrder order = requireOwnedOrder(orderId);
        if (!OrderStatus.CS_REJECTED.name().equals(order.getStatus())) {
            throw new BizException(ResultCode.ORDER_STATUS_INVALID, "只有客服退回的订单可以重新提交");
        }
        ResourceCheckReq checkReq = new ResourceCheckReq();
        checkReq.setAddress(req.getInstallAddress());
        ResourceCheckResp checked = resourceCheckService.check(checkReq);
        if (!"RESOURCE_OK".equals(checked.getStatus())) {
            throw new BizException(ResultCode.ADDRESS_NOT_COVERED, checked.getMessage());
        }
        Long uid = SecurityUtils.requireUserId();
        order.setInstallAddress(req.getInstallAddress());
        order.setExpectedInstallDate(req.getAppointmentTime());
        order.setRoomId(checked.getRoomId());
        order.setResourceStatus(checked.getStatus());
        order.setCheckRemark(checked.getMessage());
        order.setStatus(OrderStatus.PENDING_CS_CONFIRM.name());
        order.setAuditRemark(null);
        order.setUpdateBy(uid);
        if (orderMapper.updateById(order) == 0) throw versionConflict();
        upsertAppointment(orderId, req.getAppointmentTime(), null, req.getRemark(), uid, false);
        appendOrderLog(orderId, OrderStatus.CS_REJECTED.name(), OrderStatus.PENDING_CS_CONFIRM.name(),
                "客户修改后重新提交", uid);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reviewCustomerOrder(Long orderId, PortalDtos.OrderReviewReq req) {
        BroadbandOrder order = orderMapper.selectById(orderId);
        if (order == null) throw new BizException(ResultCode.ORDER_NOT_FOUND);
        if (!OrderStatus.PENDING_CS_CONFIRM.name().equals(order.getStatus())) {
            throw new BizException(ResultCode.ORDER_STATUS_INVALID, "订单不在待客服确认状态");
        }
        Long uid = SecurityUtils.requireUserId();
        String target = Boolean.TRUE.equals(req.getApproved())
                ? OrderStatus.CREATED.name() : OrderStatus.CS_REJECTED.name();
        order.setStatus(target);
        order.setCsId(uid);
        order.setAuditRemark(req.getRemark());
        order.setUpdateBy(uid);
        if (orderMapper.updateById(order) == 0) throw versionConflict();
        appendOrderLog(orderId, OrderStatus.PENDING_CS_CONFIRM.name(), target,
                Boolean.TRUE.equals(req.getApproved()) ? "客服确认受理" : "客服退回：" + nullToEmpty(req.getRemark()), uid);
    }

    public PageResp<PortalDtos.CustomerOrderSummaryVO> pageCustomerSubmittedOrders(
            long pageNum, long pageSize, String status) {
        long safeSize = Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<BroadbandOrder> q = new LambdaQueryWrapper<BroadbandOrder>()
                .eq(BroadbandOrder::getSource, "CUSTOMER_SELF")
                .orderByDesc(BroadbandOrder::getCreateTime);
        if (StringUtils.hasText(status)) q.eq(BroadbandOrder::getStatus, status);
        Page<BroadbandOrder> page = orderMapper.selectPage(new Page<>(Math.max(pageNum, 1), safeSize), q);
        PageResp<PortalDtos.CustomerOrderSummaryVO> resp = new PageResp<>();
        resp.setPageNum(page.getCurrent()); resp.setPageSize(page.getSize());
        resp.setTotal(page.getTotal()); resp.setPages(page.getPages());
        resp.setRecords(page.getRecords().stream().map(this::toOrderSummary).toList());
        return resp;
    }

    private PortalDtos.CustomerOrderSummaryVO toOrderSummary(BroadbandOrder order) {
        PortalDtos.CustomerOrderSummaryVO vo = new PortalDtos.CustomerOrderSummaryVO();
        vo.setId(order.getId()); vo.setOrderNo(order.getOrderNo());
        vo.setPackageCode(order.getPackageCode()); vo.setPackageName(order.getPackageName());
        vo.setInstallAddress(order.getInstallAddress()); vo.setStatus(order.getStatus());
        vo.setStatusLabel(customerStatusLabel(order.getStatus()));
        vo.setResourceStatus(order.getResourceStatus()); vo.setCompletedTime(order.getCompletedTime());
        vo.setCreateTime(order.getCreateTime());
        Appointment appointment = latestAppointment(order.getId());
        if (appointment != null) vo.setAppointmentTime(appointment.getAppointmentTime());
        vo.setCanResubmit(OrderStatus.CS_REJECTED.name().equals(order.getStatus()));
        vo.setCanReschedule(!Set.of(OrderStatus.INSTALLING.name(), OrderStatus.FINISHED.name(),
                OrderStatus.CLOSED.name(), OrderStatus.CANCELLED.name()).contains(order.getStatus()));
        boolean finished = OrderStatus.FINISHED.name().equals(order.getStatus())
                || OrderStatus.CLOSED.name().equals(order.getStatus());
        Long evaluationCount = evaluationMapper.selectCount(new LambdaQueryWrapper<ServiceEvaluation>()
                .eq(ServiceEvaluation::getOrderId, order.getId()));
        vo.setCanEvaluate(finished && evaluationCount == 0);
        return vo;
    }

    private String customerStatusLabel(String status) {
        if (status == null) return "未知";
        return switch (status) {
            case "PENDING_CS_CONFIRM" -> "待客服确认";
            case "CS_REJECTED" -> "客服已退回";
            case "CREATED", "AUDITED", "REJECTED" -> "审核处理中";
            case "WAIT_DISPATCH" -> "派单中";
            case "DISPATCHED" -> "待上门";
            case "INSTALLING" -> "上门安装中";
            case "FINISHED" -> "待确认/评价";
            case "CLOSED" -> "已完成";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }

    private void appendOrderLog(Long orderId, String from, String to, String remark, Long operatorId) {
        OrderAuditLog log = new OrderAuditLog();
        log.setOrderId(orderId); log.setAuditorId(operatorId);
        log.setFromStatus(from); log.setToStatus(to); log.setRemark(remark);
        log.setCreateBy(operatorId); log.setUpdateBy(operatorId);
        orderAuditLogMapper.insert(log);
    }

    /* -------------------- appointment -------------------- */

    @Transactional(rollbackFor = Exception.class)
    public Appointment rescheduleOwnAppointment(Long orderId, PortalDtos.AppointmentChangeReq req) {
        BroadbandOrder order = requireOwnedOrder(orderId);
        if (Set.of(OrderStatus.INSTALLING.name(), OrderStatus.FINISHED.name(), OrderStatus.CLOSED.name(),
                OrderStatus.CANCELLED.name()).contains(order.getStatus())) {
            throw new BizException(ResultCode.ORDER_STATUS_INVALID, "订单已开始施工或已结束，不能改期");
        }
        if (!req.getAppointmentTime().isAfter(LocalDateTime.now())) {
            throw new BizException(ResultCode.BAD_REQUEST, "预约时间必须晚于当前时间");
        }
        return upsertAppointment(orderId, req.getAppointmentTime(), null, req.getReason(),
                SecurityUtils.requireUserId(), true);
    }

    private Appointment upsertAppointment(Long orderId, LocalDateTime time, String phone, String reason,
                                          Long uid, boolean writeLog) {
        if (time == null) return latestAppointment(orderId);
        Appointment appointment = latestAppointment(orderId);
        LocalDateTime oldTime = null;
        if (appointment == null) {
            appointment = new Appointment();
            appointment.setOrderId(orderId); appointment.setAppointmentTime(time);
            appointment.setContactPhone(phone); appointment.setRemark(reason);
            appointment.setConfirmed(0); appointment.setStatus("PENDING");
            appointment.setRescheduleCount(0); appointment.setCreateBy(uid); appointment.setUpdateBy(uid);
            appointmentMapper.insert(appointment);
        } else {
            oldTime = appointment.getAppointmentTime();
            appointment.setAppointmentTime(time);
            if (phone != null) appointment.setContactPhone(phone);
            appointment.setRemark(reason);
            appointment.setStatus("PENDING"); appointment.setConfirmed(0);
            appointment.setRescheduleCount((appointment.getRescheduleCount() == null ? 0
                    : appointment.getRescheduleCount()) + 1);
            appointment.setUpdateBy(uid);
            if (appointmentMapper.updateById(appointment) == 0) throw versionConflict();
        }
        if (writeLog) {
            AppointmentChangeLog change = new AppointmentChangeLog();
            change.setAppointmentId(appointment.getId()); change.setOrderId(orderId);
            change.setOldAppointmentTime(oldTime); change.setNewAppointmentTime(time);
            change.setOperatorId(uid); change.setOperatorRole("CUSTOMER"); change.setReason(reason);
            change.setCreateBy(uid); change.setUpdateBy(uid);
            appointmentChangeLogMapper.insert(change);
        }
        return appointment;
    }

    private Appointment latestAppointment(Long orderId) {
        return appointmentMapper.selectOne(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getOrderId, orderId)
                .orderByDesc(Appointment::getCreateTime).last("LIMIT 1"));
    }

    /* -------------------- after-sales ticket -------------------- */

    @Transactional(rollbackFor = Exception.class)
    public Long createTicket(PortalDtos.TicketCreateReq req) {
        CustomerUserBinding binding = requireCurrentBinding();
        String type = req.getType().toUpperCase();
        if (!TICKET_TYPES.contains(type)) throw new BizException(ResultCode.BAD_REQUEST, "服务单类型无效");
        if (req.getOrderId() != null) requireOwnedOrder(req.getOrderId());
        CustomerServiceTicket ticket = new CustomerServiceTicket();
        ticket.setTicketNo("TK" + snowflake.nextId());
        ticket.setCustomerId(binding.getCustomerId()); ticket.setOrderId(req.getOrderId());
        ticket.setType(type); ticket.setCategory(req.getCategory()); ticket.setPriority(3);
        ticket.setDescription(req.getDescription()); ticket.setAttachments(toJson(req.getAttachments()));
        ticket.setStatus("SUBMITTED"); ticket.setCreateBy(SecurityUtils.requireUserId());
        ticket.setUpdateBy(SecurityUtils.requireUserId());
        ticketMapper.insert(ticket);
        return ticket.getId();
    }

    public PageResp<CustomerServiceTicket> pageOwnTickets(long pageNum, long pageSize, String type) {
        Long customerId = requireCurrentBinding().getCustomerId();
        LambdaQueryWrapper<CustomerServiceTicket> q = new LambdaQueryWrapper<CustomerServiceTicket>()
                .eq(CustomerServiceTicket::getCustomerId, customerId)
                .orderByDesc(CustomerServiceTicket::getCreateTime);
        if (StringUtils.hasText(type)) q.eq(CustomerServiceTicket::getType, type.toUpperCase());
        Page<CustomerServiceTicket> page = ticketMapper.selectPage(
                new Page<>(Math.max(pageNum, 1), Math.min(Math.max(pageSize, 1), 100)), q);
        return PageResp.of(page);
    }

    public CustomerServiceTicket getOwnTicket(Long id) {
        Long customerId = requireCurrentBinding().getCustomerId();
        CustomerServiceTicket ticket = ticketMapper.selectById(id);
        if (ticket == null || !customerId.equals(ticket.getCustomerId())) {
            throw new BizException(ResultCode.NOT_FOUND, "服务单不存在");
        }
        return ticket;
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmTicketResolved(Long id) {
        CustomerServiceTicket ticket = getOwnTicket(id);
        if (!"WAIT_CONFIRM".equals(ticket.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "服务单尚未进入待确认状态");
        }
        ticket.setStatus("CLOSED"); ticket.setClosedTime(LocalDateTime.now());
        ticket.setUpdateBy(SecurityUtils.requireUserId());
        if (ticketMapper.updateById(ticket) == 0) throw versionConflict();
    }

    public PageResp<CustomerServiceTicket> pageTicketsForAdmin(long pageNum, long pageSize,
                                                                String type, String status) {
        LambdaQueryWrapper<CustomerServiceTicket> q = new LambdaQueryWrapper<CustomerServiceTicket>()
                .orderByDesc(CustomerServiceTicket::getCreateTime);
        if (StringUtils.hasText(type)) q.eq(CustomerServiceTicket::getType, type.toUpperCase());
        if (StringUtils.hasText(status)) q.eq(CustomerServiceTicket::getStatus, status.toUpperCase());
        return PageResp.of(ticketMapper.selectPage(new Page<>(Math.max(pageNum, 1),
                Math.min(Math.max(pageSize, 1), 100)), q));
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleTicket(Long id, PortalDtos.TicketHandleReq req) {
        CustomerServiceTicket ticket = ticketMapper.selectById(id);
        if (ticket == null) throw new BizException(ResultCode.NOT_FOUND, "服务单不存在");
        String target = req.getStatus().toUpperCase();
        if (!TICKET_STATUSES.contains(target)) throw new BizException(ResultCode.BAD_REQUEST, "服务单状态无效");
        ticket.setStatus(target);
        ticket.setHandlerId(req.getHandlerId() == null ? SecurityUtils.requireUserId() : req.getHandlerId());
        ticket.setHandleResult(req.getHandleResult()); ticket.setUpdateBy(SecurityUtils.requireUserId());
        if ("ACCEPTED".equals(target) && ticket.getAcceptedTime() == null) ticket.setAcceptedTime(LocalDateTime.now());
        if ("WAIT_CONFIRM".equals(target)) ticket.setResolvedTime(LocalDateTime.now());
        if ("CLOSED".equals(target)) ticket.setClosedTime(LocalDateTime.now());
        if (ticketMapper.updateById(ticket) == 0) throw versionConflict();
    }

    /* -------------------- evaluation -------------------- */

    @Transactional(rollbackFor = Exception.class)
    public Long evaluateOrder(Long orderId, PortalDtos.EvaluationCreateReq req) {
        BroadbandOrder order = requireOwnedOrder(orderId);
        if (!Set.of(OrderStatus.FINISHED.name(), OrderStatus.CLOSED.name()).contains(order.getStatus())) {
            throw new BizException(ResultCode.ORDER_STATUS_INVALID, "订单完成后才能评价");
        }
        if (evaluationMapper.selectCount(new LambdaQueryWrapper<ServiceEvaluation>()
                .eq(ServiceEvaluation::getOrderId, orderId)) > 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "该订单已经评价");
        }
        WorkOrder workOrder = workOrderMapper.selectByOrderId(orderId);
        if (workOrder == null || workOrder.getInstallerId() == null) {
            throw new BizException(ResultCode.WORKORDER_NOT_FOUND, "订单没有可评价的装维人员");
        }
        ServiceEvaluation evaluation = new ServiceEvaluation();
        evaluation.setOrderId(orderId); evaluation.setWorkOrderId(workOrder.getId());
        evaluation.setCustomerId(order.getCustomerId()); evaluation.setInstallerId(workOrder.getInstallerId());
        evaluation.setOverallScore(req.getOverallScore()); evaluation.setServiceScore(req.getServiceScore());
        evaluation.setQualityScore(req.getQualityScore());
        evaluation.setPunctualityScore(req.getPunctualityScore());
        evaluation.setTags(toJson(req.getTags())); evaluation.setContent(req.getContent());
        evaluation.setStatus(1); evaluation.setCreateBy(SecurityUtils.requireUserId());
        evaluation.setUpdateBy(SecurityUtils.requireUserId());
        evaluationMapper.insert(evaluation);
        refreshInstallerScore(workOrder.getInstallerId());
        return evaluation.getId();
    }

    public ServiceEvaluation getOwnEvaluation(Long orderId) {
        requireOwnedOrder(orderId);
        return evaluationMapper.selectOne(new LambdaQueryWrapper<ServiceEvaluation>()
                .eq(ServiceEvaluation::getOrderId, orderId).last("LIMIT 1"));
    }

    private void refreshInstallerScore(Long installerId) {
        List<ServiceEvaluation> list = evaluationMapper.selectList(new LambdaQueryWrapper<ServiceEvaluation>()
                .eq(ServiceEvaluation::getInstallerId, installerId).eq(ServiceEvaluation::getStatus, 1));
        if (list.isEmpty()) return;
        double avg = list.stream().mapToInt(ServiceEvaluation::getOverallScore).average().orElse(5.0);
        InstallerProfile profile = installerProfileMapper.selectByUserId(installerId);
        if (profile != null) {
            profile.setScore(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
            installerProfileMapper.updateById(profile);
        }
    }

    /* -------------------- profile change approval -------------------- */

    @Transactional(rollbackFor = Exception.class)
    public Long requestProfileChange(PortalDtos.ProfileChangeReq req) {
        CustomerUserBinding binding = requireCurrentBinding();
        List<String> fields = new ArrayList<>();
        if (StringUtils.hasText(req.getName())) fields.add("name");
        if (StringUtils.hasText(req.getPhone())) fields.add("phone");
        if (StringUtils.hasText(req.getIdCardNo())) fields.add("idCardNo");
        if (StringUtils.hasText(req.getAddress())) fields.add("address");
        if (fields.isEmpty()) throw new BizException(ResultCode.BAD_REQUEST, "至少填写一项变更内容");
        if (profileChangeMapper.selectCount(new LambdaQueryWrapper<CustomerProfileChange>()
                .eq(CustomerProfileChange::getCustomerId, binding.getCustomerId())
                .eq(CustomerProfileChange::getStatus, "PENDING")) > 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "已有待审核的资料变更申请");
        }
        String key = orderProperties.getSm4Key();
        CustomerProfileChange change = new CustomerProfileChange();
        change.setCustomerId(binding.getCustomerId()); change.setApplicantUserId(SecurityUtils.requireUserId());
        change.setChangeFields(String.join(",", fields));
        if (StringUtils.hasText(req.getName())) change.setProposedName(CryptoUtils.sm4Encrypt(req.getName(), key));
        if (StringUtils.hasText(req.getPhone())) change.setProposedPhone(CryptoUtils.sm4Encrypt(req.getPhone(), key));
        if (StringUtils.hasText(req.getIdCardNo())) change.setProposedIdCardNo(CryptoUtils.sm4Encrypt(req.getIdCardNo(), key));
        change.setProposedAddress(req.getAddress()); change.setStatus("PENDING");
        change.setCreateBy(SecurityUtils.requireUserId()); change.setUpdateBy(SecurityUtils.requireUserId());
        profileChangeMapper.insert(change);
        return change.getId();
    }

    public List<PortalDtos.ProfileChangeVO> listOwnProfileChanges() {
        Long customerId = requireCurrentBinding().getCustomerId();
        return profileChangeMapper.selectList(new LambdaQueryWrapper<CustomerProfileChange>()
                        .eq(CustomerProfileChange::getCustomerId, customerId)
                        .orderByDesc(CustomerProfileChange::getCreateTime))
                .stream().map(this::toProfileChangeVO).toList();
    }

    public PageResp<PortalDtos.ProfileChangeVO> pageProfileChangesForAdmin(
            long pageNum, long pageSize, String status) {
        LambdaQueryWrapper<CustomerProfileChange> q = new LambdaQueryWrapper<CustomerProfileChange>()
                .orderByDesc(CustomerProfileChange::getCreateTime);
        if (StringUtils.hasText(status)) q.eq(CustomerProfileChange::getStatus, status.toUpperCase());
        Page<CustomerProfileChange> page = profileChangeMapper.selectPage(new Page<>(Math.max(pageNum, 1),
                Math.min(Math.max(pageSize, 1), 100)), q);
        PageResp<PortalDtos.ProfileChangeVO> resp = new PageResp<>();
        resp.setPageNum(page.getCurrent()); resp.setPageSize(page.getSize());
        resp.setTotal(page.getTotal()); resp.setPages(page.getPages());
        resp.setRecords(page.getRecords().stream().map(this::toProfileChangeVO).toList());
        return resp;
    }

    @Transactional(rollbackFor = Exception.class)
    public void reviewProfileChange(Long id, PortalDtos.ProfileReviewReq req) {
        CustomerProfileChange change = profileChangeMapper.selectById(id);
        if (change == null) throw new BizException(ResultCode.NOT_FOUND, "资料变更申请不存在");
        if (!"PENDING".equals(change.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "申请已经处理");
        }
        Long reviewer = SecurityUtils.requireUserId();
        if (Boolean.TRUE.equals(req.getApproved())) {
            Customer customer = customerMapper.selectById(change.getCustomerId());
            if (customer == null) throw new BizException(ResultCode.CUSTOMER_NOT_FOUND);
            if (change.getProposedName() != null) customer.setName(change.getProposedName());
            if (change.getProposedPhone() != null) customer.setPhone(change.getProposedPhone());
            if (change.getProposedIdCardNo() != null) customer.setIdCardNo(change.getProposedIdCardNo());
            if (change.getProposedAddress() != null) customer.setAddress(change.getProposedAddress());
            customer.setUpdateBy(reviewer);
            if (customerMapper.updateById(customer) == 0) throw versionConflict();
            change.setStatus("APPROVED");
        } else {
            change.setStatus("REJECTED");
        }
        change.setReviewerId(reviewer); change.setReviewRemark(req.getRemark());
        change.setReviewTime(LocalDateTime.now()); change.setUpdateBy(reviewer);
        if (profileChangeMapper.updateById(change) == 0) throw versionConflict();
    }

    private PortalDtos.ProfileChangeVO toProfileChangeVO(CustomerProfileChange c) {
        PortalDtos.ProfileChangeVO vo = new PortalDtos.ProfileChangeVO();
        vo.setId(c.getId()); vo.setCustomerId(c.getCustomerId()); vo.setChangeFields(c.getChangeFields());
        vo.setProposedName(decrypt(c.getProposedName())); vo.setProposedPhone(decrypt(c.getProposedPhone()));
        vo.setProposedIdCardNo(decrypt(c.getProposedIdCardNo())); vo.setProposedAddress(c.getProposedAddress());
        vo.setStatus(c.getStatus()); vo.setReviewerId(c.getReviewerId());
        vo.setReviewRemark(c.getReviewRemark()); vo.setReviewTime(c.getReviewTime()); vo.setCreateTime(c.getCreateTime());
        return vo;
    }

    /* -------------------- in-app messages -------------------- */

    public PageResp<Message> pageOwnMessages(long pageNum, long pageSize) {
        Long userId = SecurityUtils.requireUserId();
        return PageResp.of(messageMapper.selectPage(new Page<>(Math.max(pageNum, 1),
                        Math.min(Math.max(pageSize, 1), 100)),
                new LambdaQueryWrapper<Message>().eq(Message::getUserId, userId)
                        .orderByDesc(Message::getCreateTime)));
    }

    @Transactional(rollbackFor = Exception.class)
    public void readOwnMessage(Long id) {
        Message message = messageMapper.selectById(id);
        Long userId = SecurityUtils.requireUserId();
        if (message == null || !userId.equals(message.getUserId())) {
            throw new BizException(ResultCode.NOT_FOUND, "消息不存在");
        }
        message.setStatus("READ"); message.setUpdateBy(userId); messageMapper.updateById(message);
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new BizException(ResultCode.BAD_REQUEST, "数据格式不正确"); }
    }

    private String decrypt(String ciphertext) {
        if (ciphertext == null) return null;
        try { return CryptoUtils.sm4Decrypt(ciphertext, orderProperties.getSm4Key()); }
        catch (Exception ignored) { return ciphertext; }
    }

    private BizException versionConflict() {
        return new BizException(ResultCode.VERSION_CONFLICT, "数据已被其他用户修改，请刷新后重试");
    }

    private String nullToEmpty(String value) { return value == null ? "" : value; }
}
