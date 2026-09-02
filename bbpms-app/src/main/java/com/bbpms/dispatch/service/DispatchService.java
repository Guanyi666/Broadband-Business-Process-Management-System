package com.bbpms.dispatch.service;

import com.bbpms.common.result.PageResp;
import com.bbpms.dispatch.dto.CandidateDTO;
import com.bbpms.dispatch.dto.DispatchQueryReq;
import com.bbpms.dispatch.dto.DispatchResultDTO;
import com.bbpms.dispatch.dto.ManualDispatchReq;
import com.bbpms.dispatch.dto.ReassignReq;
import com.bbpms.dispatch.vo.DispatchRecordVO;
import com.bbpms.dispatch.vo.DispatchStatVO;

import java.util.List;

public interface DispatchService {

    DispatchResultDTO autoDispatch(Long orderId);

    DispatchResultDTO manualDispatch(ManualDispatchReq req);

    DispatchResultDTO reassign(ReassignReq req);

    /** Read-only candidate preview (no locks, no side effects). */
    List<CandidateDTO> getCandidates(Long orderId);

    PageResp<DispatchRecordVO> pageRecords(DispatchQueryReq req);

    DispatchStatVO stats(Integer days);
}