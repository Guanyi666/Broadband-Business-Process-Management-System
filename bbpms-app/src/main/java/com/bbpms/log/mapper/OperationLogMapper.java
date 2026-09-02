package com.bbpms.log.mapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bbpms.common.annotation.DataScope;
import com.bbpms.log.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
    @DataScope
    IPage<OperationLog> selectPageWithScope(IPage<OperationLog> page, @Param("ew") Wrapper<OperationLog> wrapper);
}