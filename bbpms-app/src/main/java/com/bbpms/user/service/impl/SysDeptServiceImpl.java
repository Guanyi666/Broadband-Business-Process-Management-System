package com.bbpms.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bbpms.user.entity.SysDept;
import com.bbpms.user.mapper.SysDeptMapper;
import com.bbpms.user.service.SysDeptService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements SysDeptService {

    @Override
    public List<SysDept> list() {
        return list(new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getSort));
    }

    @Override
    public List<SysDept> getTree() {
        List<SysDept> all = list();
        Map<Long, List<SysDept>> byParent = all.stream()
                .collect(Collectors.groupingBy(d -> d.getParentId() == null ? 0L : d.getParentId()));
        List<SysDept> roots = byParent.getOrDefault(0L, new ArrayList<>());
        return roots;
    }
}