package com.bbpms.order.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.bbpms.common.annotation.DataScope;
import com.bbpms.order.entity.BroadbandOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Order mapper. Every list-bearing query is annotated
 * {@link DataScope @DataScope} so the {@code DataScopeAspect} applies
 * the current user's row-level filter.
 */
@Mapper
public interface BroadbandOrderMapper extends BaseMapper<BroadbandOrder> {

    /**
     * Single-row lookup by business order number (UK).
     */
    BroadbandOrder selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * Paginated query with row-level data-scope enforced by the
     * common {@code DataScopeAspect}.
     */
    @DataScope
    IPage<BroadbandOrder> selectPageWithScope(IPage<BroadbandOrder> page,
                                              @Param(Constants.WRAPPER) Wrapper<BroadbandOrder> wrapper);

    /**
     * Status-sweep query — finds CREATED orders whose audit window has
     * elapsed and AUDITED orders waiting to be dispatched.
     */
    List<BroadbandOrder> selectByStatus(@Param("status") String status,
                                        @Param("beforeTime") LocalDateTime beforeTime,
                                        @Param("limit") int limit);

    List<BroadbandOrder> selectByCustomer(@Param("customerId") Long customerId);
}