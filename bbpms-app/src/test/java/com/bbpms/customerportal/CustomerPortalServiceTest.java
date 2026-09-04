package com.bbpms.customerportal;

import com.bbpms.common.exception.BizException;
import com.bbpms.common.security.SecurityContextHolder;
import com.bbpms.common.security.SecurityUser;
import com.bbpms.customerportal.entity.CustomerUserBinding;
import com.bbpms.customerportal.mapper.*;
import com.bbpms.customerportal.service.CustomerPortalService;
import com.bbpms.notify.mapper.MessageMapper;
import com.bbpms.order.dto.OrderDetailVO;
import com.bbpms.order.entity.BroadbandOrder;
import com.bbpms.order.mapper.*;
import com.bbpms.order.service.CustomerService;
import com.bbpms.order.service.OrderService;
import com.bbpms.resource.service.ResourceCheckService;
import com.bbpms.user.mapper.InstallerProfileMapper;
import com.bbpms.user.mapper.SysUserMapper;
import com.bbpms.user.service.SysUserService;
import com.bbpms.workorder.mapper.WorkOrderMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerPortalServiceTest {
    @Mock CustomerUserBindingMapper bindingMapper;
    @Mock BroadbandPackageMapper packageMapper;
    @Mock CustomerServiceTicketMapper ticketMapper;
    @Mock ServiceEvaluationMapper evaluationMapper;
    @Mock CustomerProfileChangeMapper profileChangeMapper;
    @Mock AppointmentChangeLogMapper appointmentChangeLogMapper;
    @Mock BroadbandOrderMapper orderMapper;
    @Mock CustomerMapper customerMapper;
    @Mock AppointmentMapper appointmentMapper;
    @Mock OrderAuditLogMapper orderAuditLogMapper;
    @Mock WorkOrderMapper workOrderMapper;
    @Mock InstallerProfileMapper installerProfileMapper;
    @Mock MessageMapper messageMapper;
    @Mock SysUserMapper sysUserMapper;
    @Mock SysUserService userService;
    @Mock CustomerService customerService;
    @Mock OrderService orderService;
    @Mock ResourceCheckService resourceCheckService;
    @Mock ApplicationEventPublisher publisher;
    @InjectMocks CustomerPortalService service;

    @BeforeEach
    void loginCustomer() {
        SecurityUser user = new SecurityUser();
        user.setUserId(101L);
        user.setUsername("customer-a");
        user.setRoles(List.of("CUSTOMER"));
        SecurityContextHolder.set(user);
    }

    @AfterEach
    void clearContext() { SecurityContextHolder.clear(); }

    @Test
    void missingBindingFailsClosed() {
        when(bindingMapper.selectOne(any())).thenReturn(null);
        BizException ex = assertThrows(BizException.class, service::requireCurrentBinding);
        assertEquals(403, ex.getCode());
    }

    @Test
    void customerCannotReadAnotherCustomersOrder() {
        stubBinding(10L);
        BroadbandOrder other = new BroadbandOrder();
        other.setId(88L);
        other.setCustomerId(20L);
        when(orderMapper.selectById(88L)).thenReturn(other);

        BizException ex = assertThrows(BizException.class, () -> service.getOwnOrder(88L));
        assertEquals(2001, ex.getCode());
        verifyNoInteractions(orderService);
    }

    @Test
    void customerCanReadBoundOrder() {
        stubBinding(10L);
        BroadbandOrder own = new BroadbandOrder();
        own.setId(77L);
        own.setCustomerId(10L);
        OrderDetailVO expected = new OrderDetailVO();
        when(orderMapper.selectById(77L)).thenReturn(own);
        when(orderService.getDetail(77L)).thenReturn(expected);

        assertEquals(expected, service.getOwnOrder(77L));
        verify(orderService).getDetail(77L);
    }

    @Test
    void customerCannotChangeAnotherCustomersAppointment() {
        stubBinding(10L);
        BroadbandOrder other = new BroadbandOrder();
        other.setId(99L);
        other.setCustomerId(11L);
        when(orderMapper.selectById(99L)).thenReturn(other);

        var req = new com.bbpms.customerportal.dto.PortalDtos.AppointmentChangeReq();
        req.setAppointmentTime(java.time.LocalDateTime.now().plusDays(1));
        BizException ex = assertThrows(BizException.class, () -> service.rescheduleOwnAppointment(99L, req));
        assertEquals(2001, ex.getCode());
        verifyNoInteractions(appointmentMapper);
    }

    private void stubBinding(Long customerId) {
        CustomerUserBinding binding = new CustomerUserBinding();
        binding.setUserId(101L);
        binding.setCustomerId(customerId);
        binding.setStatus(1);
        when(bindingMapper.selectOne(any())).thenReturn(binding);
    }
}
