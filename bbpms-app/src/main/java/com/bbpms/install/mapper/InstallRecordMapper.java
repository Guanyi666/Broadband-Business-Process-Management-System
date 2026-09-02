package com.bbpms.install.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bbpms.common.annotation.DataScope;
import com.bbpms.install.dto.InstallPageReq;
import com.bbpms.install.entity.InstallRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface InstallRecordMapper extends BaseMapper<InstallRecord> {

    /** Paged query with row-level data-scope enforcement. */
    @DataScope
    IPage<InstallRecord> selectPageWithScope(IPage<InstallRecord> page, @Param("q") InstallPageReq q);

    InstallRecord selectByWorkOrderId(@Param("workOrderId") Long workOrderId);

    List<InstallRecord> selectByInstaller(@Param("installerId") Long installerId);

    List<InstallRecord> selectPending();
}