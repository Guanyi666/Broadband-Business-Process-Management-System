package com.bbpms.workorder.service.impl;

import com.bbpms.common.enums.OrderStatus;
import com.bbpms.common.enums.WorkOrderStatus;
import com.bbpms.common.event.BbpmsEvents;
import com.bbpms.common.statemachine.WorkOrderStateMachine;
import com.bbpms.common.util.SnowflakeIdGenerator;
import com.bbpms.dispatch.config.DispatchProperties;
import com.bbpms.order.service.OrderService;
import com.bbpms.user.service.InstallerProfileService;
import com.bbpms.user.vo.InstallerVO;
import com.bbpms.workorder.config.WorkOrderProperties;
import com.bbpms.workorder.entity.WorkOrder;
import com.bbpms.workorder.entity.WorkOrderTimeline;
import com.bbpms.workorder.mapper.WorkOrderMapper;
import com.bbpms.workorder.mapper.WorkOrderTimelineMapper;
import com.bbpms.workorder.service.WorkOrderTimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderServiceImplTest {

    @Mock private WorkOrderMapper workOrderMapper;
    @Mock private WorkOrderTimelineMapper timelineMapper;
    @Mock private WorkOrderTimelineService timelineService;
    @Mock private OrderService orderService;
    @Mock private ApplicationEventPublisher publisher;
    @Mock private InstallerProfileService installerProfileService;
    private WorkOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new WorkOrderServiceImpl(
                workOrderMapper, timelineMapper, timelineService, orderService,
                new WorkOrderStateMachine(), publisher, new WorkOrderProperties(),
                installerProfileService, new DispatchProperties(), new SnowflakeIdGenerator(1, 1));
    }

    @Test
    void assignsPendingWorkOrderAndSynchronizesTimelineWorkloadAndOrder() {
        WorkOrder pending = workOrder(10L, 20L, null, WorkOrderStatus.PENDING, 0);
        WorkOrder assigned = workOrder(10L, 20L, 9L, WorkOrderStatus.DISPATCHED, 1);
        when(installerProfileService.listAvailable(5)).thenReturn(List.of(availableInstaller(9L)));
        when(workOrderMapper.selectById(10L)).thenReturn(pending, assigned);
        when(workOrderMapper.updateById(any(WorkOrder.class))).thenReturn(1);

        service.assignPending(10L, 9L, 4L, "manual dispatch");

        ArgumentCaptor<WorkOrder> update = ArgumentCaptor.forClass(WorkOrder.class);
        verify(workOrderMapper).updateById(update.capture());
        assertEquals(9L, update.getValue().getInstallerId());
        assertEquals(WorkOrderStatus.DISPATCHED, update.getValue().getStatusEnum());
        verify(installerProfileService).incrementWorkload(9L);
        verify(orderService).updateStatus(20L, OrderStatus.DISPATCHED, 4L);
        verify(timelineMapper).insert(any(WorkOrderTimeline.class));
        verify(publisher).publishEvent(any(BbpmsEvents.WorkOrderDispatchedEvent.class));
    }

    @Test
    void reassignsOwnerAndBalancesBothInstallerWorkloads() {
        WorkOrder current = workOrder(10L, 20L, 8L, WorkOrderStatus.DISPATCHED, 0);
        WorkOrder reassigned = workOrder(10L, 20L, 9L, WorkOrderStatus.DISPATCHED, 2);
        when(installerProfileService.listAvailable(5)).thenReturn(List.of(availableInstaller(9L)));
        when(workOrderMapper.selectById(10L)).thenReturn(current, reassigned);
        when(workOrderMapper.updateById(any(WorkOrder.class))).thenReturn(1);

        service.reassign(10L, 9L, 4L, "区域调整");

        ArgumentCaptor<WorkOrder> updates = ArgumentCaptor.forClass(WorkOrder.class);
        verify(workOrderMapper, times(2)).updateById(updates.capture());
        assertEquals(WorkOrderStatus.REASSIGNING, updates.getAllValues().get(0).getStatusEnum());
        assertEquals(9L, updates.getAllValues().get(1).getInstallerId());
        assertEquals(WorkOrderStatus.DISPATCHED, updates.getAllValues().get(1).getStatusEnum());
        verify(timelineMapper, times(2)).insert(any(WorkOrderTimeline.class));
        verify(installerProfileService).decrementWorkload(8L);
        verify(installerProfileService).incrementWorkload(9L);
        verify(publisher).publishEvent(any(BbpmsEvents.WorkOrderReassignedEvent.class));
    }

    private static WorkOrder workOrder(Long id, Long orderId, Long installerId,
                                       WorkOrderStatus status, int version) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(id);
        workOrder.setWorkNo("WO10");
        workOrder.setOrderId(orderId);
        workOrder.setInstallerId(installerId);
        workOrder.setStatusEnum(status);
        workOrder.setVersion(version);
        return workOrder;
    }

    private static InstallerVO availableInstaller(Long userId) {
        InstallerVO installer = new InstallerVO();
        installer.setUserId(userId);
        installer.setOnDuty(1);
        installer.setWorkload(0);
        return installer;
    }
}
