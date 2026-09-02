package com.bbpms.log.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bbpms.common.event.BbpmsEvents;
import com.bbpms.common.result.PageResp;
import com.bbpms.log.dto.LoginLogPageReq;
import com.bbpms.log.entity.LoginLog;
import com.bbpms.log.mapper.LoginLogMapper;
import com.bbpms.log.service.LoginLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class LoginLogServiceImpl extends ServiceImpl<LoginLogMapper, LoginLog> implements LoginLogService {
    @Override
    public PageResp<LoginLog> page(LoginLogPageReq req) {
        Page<LoginLog> page = new Page<>(req.getPageNum(), req.getPageSize());
        LambdaQueryWrapper<LoginLog> qw = new LambdaQueryWrapper<>();
        if (req.getUserId() != null) qw.eq(LoginLog::getUserId, req.getUserId());
        if (req.getUsername() != null && !req.getUsername().isBlank()) qw.like(LoginLog::getUsername, req.getUsername());
        qw.orderByDesc(LoginLog::getCreateTime);
        Page<LoginLog> result = baseMapper.selectPage(page, qw);
        PageResp<LoginLog> resp = new PageResp<>();
        resp.setRecords(result.getRecords());
        resp.setTotal(result.getTotal());
        resp.setPageNum(result.getCurrent());
        resp.setPageSize(result.getSize());
        resp.setPages(result.getPages());
        return resp;
    }
    @Override
    @Async
    public void record(BbpmsEvents.LoginLogEvent event) {
        LoginLog log = new LoginLog();
        log.setUserId(event.getUserId());
        log.setUsername(event.getUsername());
        log.setIp(event.getIp());
        log.setUserAgent(event.getUserAgent());
        log.setStatus(event.getStatus());
        log.setMessage(event.getMessage());
        save(log);
    }
}