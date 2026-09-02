package com.bbpms.log.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bbpms.common.event.BbpmsEvents;
import com.bbpms.common.result.PageResp;
import com.bbpms.common.util.JsonUtils;
import com.bbpms.log.dto.OperationLogPageReq;
import com.bbpms.log.dto.StatResp;
import com.bbpms.log.entity.OperationLog;
import com.bbpms.log.mapper.OperationLogMapper;
import com.bbpms.log.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {
    @Override
    public PageResp<OperationLog> page(OperationLogPageReq req) {
        Page<OperationLog> page = new Page<>(req.getPageNum(), req.getPageSize());
        LambdaQueryWrapper<OperationLog> qw = new LambdaQueryWrapper<>();
        if (req.getUserId() != null) qw.eq(OperationLog::getUserId, req.getUserId());
        if (req.getModule() != null && !req.getModule().isBlank()) qw.eq(OperationLog::getModule, req.getModule());
        if (req.getAction() != null && !req.getAction().isBlank()) qw.like(OperationLog::getAction, req.getAction());
        qw.orderByDesc(OperationLog::getCreateTime);
        Page<OperationLog> result = baseMapper.selectPage(page, qw);
        PageResp<OperationLog> resp = new PageResp<>();
        resp.setRecords(result.getRecords());
        resp.setTotal(result.getTotal());
        resp.setPageNum(result.getCurrent());
        resp.setPageSize(result.getSize());
        resp.setPages(result.getPages());
        return resp;
    }
    @Override
    public List<StatResp> stat(Integer days) {
        return new ArrayList<>();
    }
    @Override
    @Async
    public void record(BbpmsEvents.OperationLogEvent event) {
        OperationLog log = new OperationLog();
        log.setUserId(event.getUserId());
        log.setUsername(event.getUsername());
        log.setModule(event.getModule());
        log.setAction(event.getAction());
        log.setRequestUri(event.getRequestUri());
        log.setMethod(event.getMethod());
        log.setIp(event.getIp());
        log.setUserAgent(event.getUserAgent());
        log.setCostMs(event.getCostMs() == null ? null : event.getCostMs().intValue());
        log.setStatus(event.getStatus() == null ? 1 : event.getStatus());
        log.setError(event.getError());
        if (event.getParams() != null) log.setParams(JsonUtils.toJson(event.getParams()));
        save(log);
    }
}