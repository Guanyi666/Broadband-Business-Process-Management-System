package com.bbpms.dispatch.service;

import com.bbpms.dispatch.entity.DispatchRule;
import com.bbpms.dispatch.vo.DispatchRuleVO;

public interface DispatchRuleService {

    /** Returns the currently active rule (the first enabled row), seeded if empty. */
    DispatchRule getActive();

    /** Persist an updated rule. */
    DispatchRule update(DispatchRule rule);

    /** Convert entity to view. */
    DispatchRuleVO toVO(DispatchRule rule);
}