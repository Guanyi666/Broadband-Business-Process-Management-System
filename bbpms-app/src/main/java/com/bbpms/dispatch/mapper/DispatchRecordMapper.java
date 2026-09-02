package com.bbpms.dispatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bbpms.common.annotation.DataScope;
import com.bbpms.dispatch.dto.DispatchQueryReq;
import com.bbpms.dispatch.entity.DispatchRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DispatchRecordMapper extends BaseMapper<DispatchRecord> {

    /**
     * Paged query with row-level data-scope enforcement. The {@code @DataScope}
     * aspect rewrites the SQL to inject dept / self filters based on the
     * current security context.
     */
    @DataScope
    IPage<DispatchRecord> selectPageWithScope(IPage<DispatchRecord> page, @Param("q") DispatchQueryReq q);

    List<DispatchRecord> selectByWorkOrderId(@Param("workOrderId") Long workOrderId);

    List<DispatchRecord> selectByInstaller(@Param("installerId") Long installerId);

    List<DispatchRecord> selectRecent(@Param("days") int days);
}