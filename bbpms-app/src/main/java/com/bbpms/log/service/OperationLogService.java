package com.bbpms.log.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bbpms.common.event.BbpmsEvents;
import com.bbpms.common.result.PageResp;
import com.bbpms.log.dto.OperationLogPageReq;
import com.bbpms.log.dto.StatResp;
import com.bbpms.log.entity.OperationLog;
import java.util.List;
public interface OperationLogService extends IService<OperationLog> {
    PageResp<OperationLog> page(OperationLogPageReq req);
    List<StatResp> stat(Integer days);
    void record(BbpmsEvents.OperationLogEvent event);
}