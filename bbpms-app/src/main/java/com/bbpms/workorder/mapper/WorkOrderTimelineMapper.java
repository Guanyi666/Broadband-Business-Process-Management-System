package com.bbpms.workorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bbpms.workorder.entity.WorkOrderTimeline;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Timeline (append-only audit log) mapper. XML resides under
 * {@code classpath:/mapper/WorkOrderTimelineMapper.xml}.
 */
@Mapper
public interface WorkOrderTimelineMapper extends BaseMapper<WorkOrderTimeline> {

    /**
     * All timeline rows for a work order, ordered by create_time asc.
     */
    List<WorkOrderTimeline> selectByWorkOrderId(@Param("workOrderId") Long workOrderId);

    /**
     * All timeline rows whose {@code work_order_id} resolves back to the
     * source {@code orderId}. Used by the order module's composite timeline.
     */
    List<WorkOrderTimeline> selectByOrderId(@Param("orderId") Long orderId);

    /** Bulk insert (used when back-filling historical rows). */
    int batchInsert(@Param("list") List<WorkOrderTimeline> list);
}
