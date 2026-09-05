package com.bbpms.workorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bbpms.common.annotation.DataScope;
import com.bbpms.workorder.dto.WorkOrderQueryReq;
import com.bbpms.workorder.entity.WorkOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Work-order header mapper. List / search methods are annotated with
 * {@link DataScope} so the {@code DataScopeAspect} can rewrite the SQL
 * based on the caller's data scope.
 */
@Mapper
public interface WorkOrderMapper extends BaseMapper<WorkOrder> {

    /** Single-row lookup by the natural unique key {@code work_no}. */
    WorkOrder selectByWorkNo(@Param("workNo") String workNo);

    /** Single-row lookup by source order id. Used for the idempotent-create guard. */
    WorkOrder selectByOrderId(@Param("orderId") Long orderId);

    /** Installer's current queue / history filtered by status. */
    List<WorkOrder> selectByInstallerAndStatus(@Param("installerId") Long installerId,
                                               @Param("status") String status);

    /**
     * Paginated query with data-scope rewriting. The aspect adds a
     * {@code WHERE} clause depending on the caller's data scope.
     */
    @DataScope
    IPage<WorkOrder> selectPageWithScope(@Param("page") IPage<WorkOrder> page,
                                         @Param("req") WorkOrderQueryReq req);

    /**
     * Paginated query restricted to a single installer (used by
     * {@code GET /api/work-orders/my}).
     */
    IPage<WorkOrder> selectPageByInstaller(@Param("page") IPage<WorkOrder> page,
                                           @Param("installerId") Long installerId,
                                           @Param("status") String status);

    /** DISPATCHED rows older than the SLA — re-assigned by the sweep job. */
    List<WorkOrder> selectExpired(@Param("olderThan") LocalDateTime olderThan,
                                  @Param("limit") Integer limit);

    /** Bulk fetch by id list (timeline rendering helper). */
    List<WorkOrder> selectByIds(@Param("ids") List<Long> ids);

    /** Explicit NULL assignment; MyBatis-Plus skips null fields in updateById. */
    @Update("""
            UPDATE work_order
            SET installer_id = NULL,
                status = 'PENDING',
                dispatch_time = NULL,
                update_by = #{operatorId},
                update_time = CURRENT_TIMESTAMP,
                version = version + 1
            WHERE id = #{id} AND version = #{version} AND deleted = 0
            """)
    int returnToPending(@Param("id") Long id,
                        @Param("version") Integer version,
                        @Param("operatorId") Long operatorId);
}
