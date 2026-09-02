package com.bbpms.workorder.service;

import com.bbpms.workorder.entity.WorkOrderTimeline;
import com.bbpms.workorder.vo.WorkOrderTimelineVO;

import java.util.List;

/**
 * Timeline lookup. Pure read service — append-only writes go through
 * {@code WorkOrderStateService}.
 */
public interface WorkOrderTimelineService {

    /** All timeline rows for a work order, oldest-first. */
    List<WorkOrderTimelineVO> getTimeline(Long workOrderId);

    /**
     * Cross-module helper used by {@code OrderTimelineService} to fold
     * work-order events into the order's composite timeline.
     */
    List<WorkOrderTimelineVO> listByOrderId(Long orderId);

    /** Internal accessor for service-to-service raw rows. */
    List<WorkOrderTimeline> listRaw(Long workOrderId);
}
