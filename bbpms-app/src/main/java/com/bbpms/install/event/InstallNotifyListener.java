package com.bbpms.install.event;

import com.bbpms.common.event.BbpmsEvents;
import com.bbpms.common.util.CryptoUtils;
import com.bbpms.customerportal.entity.CustomerUserBinding;
import com.bbpms.customerportal.mapper.CustomerUserBindingMapper;
import com.bbpms.order.config.OrderProperties;
import com.bbpms.order.entity.BroadbandOrder;
import com.bbpms.order.entity.Customer;
import com.bbpms.order.mapper.BroadbandOrderMapper;
import com.bbpms.order.mapper.CustomerMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Bridges InstallCompletedEvent into a customer-facing NotifyEvent. The
 * notification module is the real consumer — this just builds the payload
 * asynchronously off the install transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InstallNotifyListener {

    private final ApplicationEventPublisher publisher;
    private final BroadbandOrderMapper orderMapper;
    private final CustomerMapper customerMapper;
    private final CustomerUserBindingMapper bindingMapper;
    private final OrderProperties orderProperties;

    @Async
    @EventListener
    public void onInstallCompleted(BbpmsEvents.InstallCompletedEvent event) {
        if (event == null || event.getWorkOrderId() == null) return;
        log.info("InstallNotifyListener fan-out for workOrderId={}", event.getWorkOrderId());
        Map<String, Object> params = new HashMap<>();
        params.put("workOrderId", event.getWorkOrderId());
        params.put("orderId", event.getOrderId());
        params.put("onuMac", event.getOnuMac());
        BroadbandOrder order = event.getOrderId() == null ? null : orderMapper.selectById(event.getOrderId());
        if (order == null) return;
        params.put("orderNo", order.getOrderNo());

        CustomerUserBinding binding = bindingMapper.selectOne(new LambdaQueryWrapper<CustomerUserBinding>()
                .eq(CustomerUserBinding::getCustomerId, order.getCustomerId())
                .eq(CustomerUserBinding::getStatus, 1).last("LIMIT 1"));
        if (binding != null) {
            publisher.publishEvent(new BbpmsEvents.NotifyEvent(
                    "INAPP", null, null, binding.getUserId(),
                    "INSTALL_COMPLETED_INAPP", params));
        }

        Customer customer = customerMapper.selectById(order.getCustomerId());
        if (customer != null && customer.getPhone() != null) {
            String phone = customer.getPhone();
            try { phone = CryptoUtils.sm4Decrypt(phone, orderProperties.getSm4Key()); }
            catch (Exception ignored) { /* legacy demo rows may still contain plaintext */ }
            publisher.publishEvent(new BbpmsEvents.NotifyEvent(
                    "SMS", phone, null, binding == null ? null : binding.getUserId(),
                    "INSTALL_COMPLETED_SMS", params));
        }
    }
}
