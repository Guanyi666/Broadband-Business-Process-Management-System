package com.bbpms.order.service.impl;

import com.bbpms.common.enums.WorkOrderStatus;
import com.bbpms.order.dto.OrderTimelineVO;
import com.bbpms.order.entity.OrderAuditLog;
import com.bbpms.order.mapper.OrderAuditLogMapper;
import com.bbpms.user.entity.SysUser;
import com.bbpms.user.mapper.SysUserMapper;
import com.bbpms.workorder.service.WorkOrderTimelineService;
import com.bbpms.workorder.vo.WorkOrderTimelineVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderTimelineServiceImplTest {

    @Test
    void returnsMergedBusinessAndOperatorFieldsForDualTrackTimeline() {
        OrderAuditLogMapper auditMapper = mock(OrderAuditLogMapper.class);
        WorkOrderTimelineService workOrderTimelineService = mock(WorkOrderTimelineService.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        OrderTimelineServiceImpl service = new OrderTimelineServiceImpl(
                auditMapper, workOrderTimelineService, userMapper);

        LocalDateTime auditTime = LocalDateTime.of(2026, 9, 5, 9, 0);
        OrderAuditLog audit = new OrderAuditLog();
        audit.setOrderId(10L);
        audit.setAuditorId(20L);
        audit.setFromStatus("CREATED");
        audit.setToStatus("AUDITED");
        audit.setRemark("资料完整");
        audit.setCreateTime(auditTime);

        WorkOrderTimelineVO work = new WorkOrderTimelineVO();
        work.setWorkOrderId(30L);
        work.setFromStatus(WorkOrderStatus.PENDING);
        work.setFromStatusDesc("待派单");
        work.setToStatus(WorkOrderStatus.DISPATCHED);
        work.setToStatusDesc("已派单");
        work.setOperatorId(21L);
        work.setOperatorName("王调度");
        work.setOperatorRole("DISPATCHER");
        work.setRemark("系统推荐后确认");
        work.setCreateTime(auditTime.plusMinutes(8));

        SysUser auditor = new SysUser();
        auditor.setId(20L);
        auditor.setUsername("cs1");
        auditor.setRealName("李客服");

        when(auditMapper.selectByOrderId(10L)).thenReturn(List.of(audit));
        when(workOrderTimelineService.listByOrderId(10L)).thenReturn(List.of(work));
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(auditor));

        List<OrderTimelineVO> result = service.getTimeline(10L);

        assertEquals(2, result.size());
        OrderTimelineVO orderEvent = result.get(0);
        assertEquals("ORDER_AUDIT", orderEvent.getSource());
        assertEquals("AUDIT_PASS", orderEvent.getEventType());
        assertEquals("CREATED", orderEvent.getFromStatus());
        assertEquals("AUDITED", orderEvent.getToStatus());
        assertEquals("李客服", orderEvent.getOperatorName());
        assertEquals("ORDER_OPERATOR", orderEvent.getOperatorRole());
        assertEquals("资料完整", orderEvent.getRemark());
        assertTrue(orderEvent.getDescription().contains("→"));

        OrderTimelineVO workEvent = result.get(1);
        assertEquals("WORKORDER", workEvent.getSource());
        assertEquals("PENDING", workEvent.getFromStatus());
        assertEquals("DISPATCHED", workEvent.getToStatus());
        assertEquals("王调度", workEvent.getOperatorName());
        assertEquals("DISPATCHER", workEvent.getOperatorRole());
    }
}
