package com.bbpms.workorder.service.impl;

import com.bbpms.common.enums.WorkOrderStatus;
import com.bbpms.user.entity.SysUser;
import com.bbpms.user.mapper.SysUserMapper;
import com.bbpms.workorder.entity.WorkOrderTimeline;
import com.bbpms.workorder.mapper.WorkOrderTimelineMapper;
import com.bbpms.workorder.vo.WorkOrderTimelineVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkOrderTimelineServiceImplTest {

    @Test
    void enrichesOperatorNameAndPreservesTransitionDetails() {
        WorkOrderTimelineMapper timelineMapper = mock(WorkOrderTimelineMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        WorkOrderTimelineServiceImpl service = new WorkOrderTimelineServiceImpl(timelineMapper, userMapper);

        WorkOrderTimeline row = new WorkOrderTimeline();
        row.setId(1L);
        row.setWorkOrderId(2L);
        row.setFromStatusEnum(WorkOrderStatus.DISPATCHED);
        row.setToStatusEnum(WorkOrderStatus.ACCEPTED);
        row.setOperatorId(3L);
        row.setOperatorRole("INSTALLER");
        row.setRemark("装维确认接单");
        row.setCreateTime(LocalDateTime.of(2026, 9, 5, 10, 0));

        SysUser installer = new SysUser();
        installer.setId(3L);
        installer.setUsername("installer1");
        installer.setRealName("张装维");

        when(timelineMapper.selectByWorkOrderId(2L)).thenReturn(List.of(row));
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(installer));

        WorkOrderTimelineVO result = service.getTimeline(2L).get(0);

        assertEquals(WorkOrderStatus.DISPATCHED, result.getFromStatus());
        assertEquals(WorkOrderStatus.ACCEPTED, result.getToStatus());
        assertEquals("张装维", result.getOperatorName());
        assertEquals("INSTALLER", result.getOperatorRole());
        assertEquals("装维确认接单", result.getRemark());
    }
}
