package com.bbpms.resource.service;

import com.bbpms.resource.dto.ResourceCheckReq;
import com.bbpms.resource.vo.ResourceCheckResp;

/** 资源核查服务：下单前判断地址可装性。 */
public interface ResourceCheckService {

    /** 核查安装地址，返回三态：RESOURCE_OK / RESOURCE_INSUFFICIENT / NO_COVERAGE。 */
    ResourceCheckResp check(ResourceCheckReq req);
}