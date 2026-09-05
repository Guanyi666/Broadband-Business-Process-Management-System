package com.bbpms.customerportal.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bbpms.common.enums.OrderStatus;
import com.bbpms.common.enums.ResultCode;
import com.bbpms.common.event.BbpmsEvents;
import com.bbpms.common.exception.BizException;
import com.bbpms.common.util.RedisUtils;
import com.bbpms.customerportal.entity.CustomerUserBinding;
import com.bbpms.customerportal.mapper.CustomerUserBindingMapper;
import com.bbpms.notify.entity.Message;
import com.bbpms.notify.mapper.MessageMapper;
import com.bbpms.order.entity.BroadbandOrder;
import com.bbpms.order.mapper.BroadbandOrderMapper;
import com.bbpms.user.entity.SysUser;
import com.bbpms.user.mapper.SysUserMapper;
import com.bbpms.workorder.entity.WorkOrder;
import com.bbpms.workorder.mapper.WorkOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 客户「超时催单」服务。
 *
 * <ul>
 *   <li>催单门槛：订单在可催单状态（待派单/已派单/安装中）停留超过 {@link #THRESHOLD_MINUTES} 分钟才允许催单；</li>
 *   <li>防刷冷却：同一订单催单后 {@link #COOLDOWN_MINUTES} 分钟内不可重复催单（Redis key 控制）；</li>
 *   <li>通知：向订单对应客服/装维人员发送站内信（INAPP），并落一条催单日志到 message 表。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UrgeService {

    /** 催单门槛（分钟）：状态停留超过该时长才可催单 */
    public static final int THRESHOLD_MINUTES = 15;
    /** 防刷冷却（分钟） */
    public static final int COOLDOWN_MINUTES = 15;
    /** 冷却 Redis key 前缀（公开供进度接口查询剩余秒数） */
    public static final String COOLDOWN_KEY_PREFIX_PUBLIC = "urge:cooldown:order:";
    /** 冷却 Redis key 前缀 */
    private static final String COOLDOWN_KEY_PREFIX = COOLDOWN_KEY_PREFIX_PUBLIC;

    private final RedisUtils redisUtils;
    private final CustomerUserBindingMapper bindingMapper;
    private final BroadbandOrderMapper orderMapper;
    private final WorkOrderMapper workOrderMapper;
    private final SysUserMapper sysUserMapper;
    private final MessageMapper messageMapper;
    private final ApplicationEventPublisher publisher;

    /**
     * 查询订单催单状态（供进度接口附带返回）。
     *
     * @return [0]=canUrge, [1]=cooling
     */
    public boolean[] urgeState(Long orderId) {
        String cooldown = redisUtils.get(COOLDOWN_KEY_PREFIX + orderId);
        if (cooldown != null) {
            return new boolean[]{false, true};
        }
        BroadbandOrder order = orderMapper.selectById(orderId);
        if (order == null || !canUrgeStatus(order.getStatus())) {
            return new boolean[]{false, false};
        }
        LocalDateTime since = anchorTime(order);
        if (since == null) return new boolean[]{false, false};
        boolean overdue = Duration.between(since, LocalDateTime.now()).toMinutes() >= THRESHOLD_MINUTES;
        return new boolean[]{overdue, false};
    }

    /** 冷却剩余秒数（无冷却返回 0）。 */
    public long remainingCooldownSeconds(Long orderId) {
        return redisUtils.getExpireSeconds(COOLDOWN_KEY_PREFIX + orderId);
    }

    /**
     * 执行催单：冷却校验 → 记录日志 → 通知客服/装维。
     */
    @Transactional(rollbackFor = Exception.class)
    public void urge(Long orderId, Long customerUserId) {
        // 1. 冷却校验（Redis setIfAbsent，TTL 15 分钟）
        String key = COOLDOWN_KEY_PREFIX + orderId;
        boolean acquired = redisUtils.tryLock(key, String.valueOf(System.currentTimeMillis()),
                COOLDOWN_MINUTES, TimeUnit.MINUTES);
        if (!acquired) {
            throw new BizException(ResultCode.TOO_FREQUENT, "您已催单过，请稍后再试（15 分钟内仅可催单一次）");
        }

        try {
            // 2. 归属与状态校验
            BroadbandOrder order = orderMapper.selectById(orderId);
            if (order == null) throw new BizException(ResultCode.ORDER_NOT_FOUND);
            if (!canUrgeStatus(order.getStatus())) {
                redisUtils.del(key);
                throw new BizException(ResultCode.ORDER_STATUS_INVALID, "当前订单状态暂不支持催单");
            }
            LocalDateTime since = anchorTime(order);
            if (since == null) {
                redisUtils.del(key);
                throw new BizException(ResultCode.ORDER_STATUS_INVALID, "当前订单状态暂不支持催单");
            }
            if (Duration.between(since, LocalDateTime.now()).toMinutes() < THRESHOLD_MINUTES) {
                redisUtils.del(key);
                throw new BizException(ResultCode.BAD_REQUEST, "订单刚进入当前状态，暂未达到催单条件");
            }

            // 3. 通知客服/装维 + 落催单日志
            notifyStaff(order);
            insertUrgeLog(order, customerUserId);
            log.info("客户催单 orderId={} orderNo={} customerUserId={}", orderId, order.getOrderNo(), customerUserId);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            redisUtils.del(key); // 异常回滚冷却，避免误锁
            throw e;
        }
    }

    /* -------------------- 内部 -------------------- */

    /** 可催单状态：待派单 / 已派单 / 安装中（等待装维上门）。 */
    private boolean canUrgeStatus(String status) {
        return OrderStatus.WAIT_DISPATCH.name().equals(status)
                || OrderStatus.DISPATCHED.name().equals(status)
                || OrderStatus.INSTALLING.name().equals(status);
    }

    /** 当前状态停留起始时间：取最近一次真实流转时间；无流转则取创建时间。 */
    private LocalDateTime anchorTime(BroadbandOrder order) {
        String status = order.getStatus();
        if (OrderStatus.WAIT_DISPATCH.name().equals(status)) {
            return order.getAuditTime() != null ? order.getAuditTime() : order.getCreateTime();
        }
        if (OrderStatus.DISPATCHED.name().equals(status)) {
            return order.getDispatchTime() != null ? order.getDispatchTime()
                    : (order.getAuditTime() != null ? order.getAuditTime() : order.getCreateTime());
        }
        if (OrderStatus.INSTALLING.name().equals(status)) {
            return order.getDispatchTime() != null ? order.getDispatchTime()
                    : (order.getAuditTime() != null ? order.getAuditTime() : order.getCreateTime());
        }
        return order.getCreateTime();
    }

    /** 通知对应客服（WAIT_DISPATCH）或装维（DISPATCHED/INSTALLING），并记录站内信。 */
    private void notifyStaff(BroadbandOrder order) {
        String status = order.getStatus();
        Long targetUserId = null;
        String roleLabel;
        if (OrderStatus.WAIT_DISPATCH.name().equals(status)) {
            targetUserId = order.getCsId();
            roleLabel = "客服";
        } else {
            // DISPATCHED / INSTALLING：通知装维（取该订单主工单的装维人员）
            Long installerId = null;
            try {
                WorkOrder wo = workOrderMapper.selectByOrderId(order.getId());
                if (wo != null) installerId = wo.getInstallerId();
            } catch (Exception ex) {
                log.warn("查询订单工单装维失败: {}", ex.getMessage());
            }
            targetUserId = installerId != null ? installerId : order.getCsId();
            roleLabel = installerId != null ? "装维" : "客服";
        }

        String targetName = "相关处理人员";
        if (targetUserId != null) {
            SysUser u = sysUserMapper.selectById(targetUserId);
            if (u != null) {
                targetName = (u.getNickname() != null && !u.getNickname().isBlank())
                        ? u.getNickname()
                        : (u.getRealName() != null && !u.getRealName().isBlank()) ? u.getRealName() : u.getUsername();
            }
        }

        // 站内信（INAPP）
        if (targetUserId != null) {
            Message msg = new Message();
            msg.setUserId(targetUserId);
            msg.setChannel("INAPP");
            msg.setContent("客户催单：订单 " + order.getOrderNo() + " 等待处理较久，请尽快处理（" + roleLabel + "关注）");
            msg.setStatus("PENDING");
            messageMapper.insert(msg);
            publisher.publishEvent(new BbpmsEvents.NotifyEvent("INAPP", null, null,
                    targetUserId, "URGE_REMIND", Map.of("orderNo", order.getOrderNo(), "orderId", order.getId())));
        }
        log.info("催单已通知 {} 目标 userId={} orderNo={}", roleLabel, targetUserId, order.getOrderNo());
    }

    /** 记录催单日志（同样走 message 表，模板标识 URGE_LOG，收件人为催单的客户账号）。 */
    private void insertUrgeLog(BroadbandOrder order, Long customerUserId) {
        if (customerUserId == null) return;
        Message logMsg = new Message();
        logMsg.setUserId(customerUserId);
        logMsg.setChannel("INAPP");
        logMsg.setTemplateCode("URGE_LOG");
        logMsg.setContent("已收到您对订单 " + order.getOrderNo() + " 的催单请求，正在加急处理中");
        logMsg.setStatus("READ");
        messageMapper.insert(logMsg);
    }

    /** 客户绑定查询（催单时校验归属）。 */
    public void assertOwnedOrder(Long orderId, Long customerUserId) {
        CustomerUserBinding binding = bindingMapper.selectOne(new LambdaQueryWrapper<CustomerUserBinding>()
                .eq(CustomerUserBinding::getUserId, customerUserId)
                .eq(CustomerUserBinding::getStatus, 1)
                .last("LIMIT 1"));
        if (binding == null) throw new BizException(ResultCode.FORBIDDEN, "当前账号未绑定客户资料");
        BroadbandOrder order = orderMapper.selectById(orderId);
        if (order == null || !binding.getCustomerId().equals(order.getCustomerId())) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
    }
}
