package com.bbpms.dispatch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bbpms.dispatch.config.DispatchProperties;
import com.bbpms.dispatch.entity.DispatchRule;
import com.bbpms.dispatch.mapper.DispatchRuleMapper;
import com.bbpms.dispatch.service.DispatchRuleService;
import com.bbpms.dispatch.vo.DispatchRuleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchRuleServiceImpl implements DispatchRuleService {

    private final DispatchRuleMapper ruleMapper;
    private final DispatchProperties props;

    @Override
    public DispatchRule getActive() {
        DispatchRule r = ruleMapper.selectActive();
        if (r == null) {
            r = seedDefaultRule();
        }
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DispatchRule update(DispatchRule rule) {
        if (rule == null || rule.getId() == null) {
            throw new IllegalArgumentException("rule.id 不能为空");
        }
        ruleMapper.updateById(rule);
        return ruleMapper.selectById(rule.getId());
    }

    @Override
    public DispatchRuleVO toVO(DispatchRule rule) {
        DispatchRuleVO vo = new DispatchRuleVO();
        if (rule == null) {
            // Fully-defaulted empty view
            vo.setName("DEFAULT");
            Map<String, Integer> w = new LinkedHashMap<>();
            w.put("distance", props.getWeightsDistance());
            w.put("load",     props.getWeightsLoad());
            w.put("skill",    props.getWeightsSkill());
            w.put("rating",   props.getWeightsRating());
            vo.setWeights(w);
            vo.setRadiusKm(props.getRadiusKm());
            vo.setEnabled(1);
            vo.setWeightsSum(sum(w));
            return vo;
        }
        vo.setId(rule.getId());
        vo.setName(rule.getName());
        Map<String, Integer> w = new LinkedHashMap<>();
        w.put("distance", rule.getWeightDistance());
        w.put("load",     rule.getWeightLoad());
        w.put("skill",    rule.getWeightSkill());
        w.put("rating",   rule.getWeightRating());
        vo.setWeights(w);
        vo.setRadiusKm(rule.getRadiusKm());
        vo.setEnabled(rule.getEnabled());
        vo.setWeightsSum(sum(w));
        return vo;
    }

    /** Build an in-memory default rule (not persisted) when the DB is empty. */
    private DispatchRule seedDefaultRule() {
        DispatchRule r = new DispatchRule();
        r.setName("DEFAULT");
        r.setWeightDistance(props.getWeightsDistance());
        r.setWeightLoad(props.getWeightsLoad());
        r.setWeightSkill(props.getWeightsSkill());
        r.setWeightRating(props.getWeightsRating());
        r.setRadiusKm(props.getRadiusKm());
        r.setEnabled(1);
        return r;
    }

    private static Integer sum(Map<String, Integer> w) {
        if (w == null) return 0;
        int s = 0;
        for (Integer v : w.values()) if (v != null) s += v;
        return s;
    }
}