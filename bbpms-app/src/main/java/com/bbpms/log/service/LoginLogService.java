package com.bbpms.log.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bbpms.common.event.BbpmsEvents;
import com.bbpms.common.result.PageResp;
import com.bbpms.log.dto.LoginLogPageReq;
import com.bbpms.log.entity.LoginLog;
public interface LoginLogService extends IService<LoginLog> {
    PageResp<LoginLog> page(LoginLogPageReq req);
    void record(BbpmsEvents.LoginLogEvent event);
}