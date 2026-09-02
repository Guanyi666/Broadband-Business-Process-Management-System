package com.bbpms.log.mapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bbpms.common.annotation.DataScope;
import com.bbpms.log.entity.LoginLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {
    @DataScope
    IPage<LoginLog> selectPageWithScope(IPage<LoginLog> page, @Param("ew") Wrapper<LoginLog> wrapper);
}