package com.bbpms.workorder.service.impl;

import com.bbpms.common.enums.WorkOrderStatus;
import com.bbpms.workorder.entity.WorkOrderTimeline;
import com.bbpms.workorder.mapper.WorkOrderTimelineMapper;
import com.bbpms.workorder.service.WorkOrderTimelineService;
import com.bbpms.workorder.vo.WorkOrderTimelineVO;
import com.bbpms.user.entity.SysUser;
import com.bbpms.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read-only timeline accessor. Append-only writes go through
 * {@code WorkOrderServiceImpl#appendTimeline}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderTimelineServiceImpl implements WorkOrderTimelineService {

    private final WorkOrderTimelineMapper timelineMapper;
    private final SysUserMapper userMapper;

    @Override
    public List<WorkOrderTimelineVO> getTimeline(Long workOrderId) {
        if (workOrderId == null) return Collections.emptyList();
        List<WorkOrderTimeline> rows = timelineMapper.selectByWorkOrderId(workOrderId);
        if (rows == null || rows.isEmpty()) return Collections.emptyList();
        return toVOList(rows);
    }

    @Override
    public List<WorkOrderTimelineVO> listByOrderId(Long orderId) {
        if (orderId == null) return Collections.emptyList();
        List<WorkOrderTimeline> rows = timelineMapper.selectByOrderId(orderId);
        if (rows == null || rows.isEmpty()) return Collections.emptyList();
        return toVOList(rows);
    }

    @Override
    public List<WorkOrderTimeline> listRaw(Long workOrderId) {
        if (workOrderId == null) return Collections.emptyList();
        return timelineMapper.selectByWorkOrderId(workOrderId);
    }

    private WorkOrderTimelineVO toVO(WorkOrderTimeline t) {
        WorkOrderTimelineVO vo = new WorkOrderTimelineVO();
        vo.setId(t.getId());
        vo.setWorkOrderId(t.getWorkOrderId());
        vo.setFromStatus(t.getFromStatusEnum());
        vo.setFromStatusDesc(desc(t.getFromStatusEnum()));
        vo.setToStatus(t.getToStatusEnum());
        vo.setToStatusDesc(desc(t.getToStatusEnum()));
        vo.setOperatorId(t.getOperatorId());
        vo.setOperatorName(null);   // joined from bbpms-user when needed
        vo.setOperatorRole(t.getOperatorRole());
        vo.setRemark(t.getRemark());
        vo.setCreateTime(t.getCreateTime());
        return vo;
    }

    private List<WorkOrderTimelineVO> toVOList(List<WorkOrderTimeline> rows) {
        List<WorkOrderTimelineVO> result = rows.stream().map(this::toVO).collect(Collectors.toList());
        List<Long> operatorIds = result.stream()
                .map(WorkOrderTimelineVO::getOperatorId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (operatorIds.isEmpty()) return result;

        try {
            Map<Long, SysUser> users = userMapper.selectBatchIds(operatorIds).stream()
                    .collect(Collectors.toMap(SysUser::getId, Function.identity(), (left, right) -> left));
            for (WorkOrderTimelineVO item : result) {
                SysUser user = users.get(item.getOperatorId());
                if (user != null) {
                    item.setOperatorName(user.getRealName() == null || user.getRealName().isBlank()
                            ? user.getUsername() : user.getRealName());
                }
            }
        } catch (Exception ex) {
            log.warn("Unable to enrich work-order timeline operator names: {}", ex.getMessage());
        }
        return result;
    }

    private String desc(WorkOrderStatus s) {
        if (s == null) return null;
        String d = s.getDesc();
        return d == null || d.isBlank() ? s.name() : d;
    }
}
