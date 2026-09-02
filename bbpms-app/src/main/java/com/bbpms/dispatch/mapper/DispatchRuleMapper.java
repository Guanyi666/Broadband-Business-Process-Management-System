package com.bbpms.dispatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bbpms.dispatch.entity.DispatchRule;
import org.apache.ibatis.annotations.Select;

public interface DispatchRuleMapper extends BaseMapper<DispatchRule> {

    /** Returns the first enabled rule ordered by id. */
    @Select("SELECT * FROM dispatch_rule WHERE enabled = 1 ORDER BY id ASC LIMIT 1")
    DispatchRule selectActive();
}