package com.bbpms.install.service;

import com.bbpms.common.result.PageResp;
import com.bbpms.install.dto.InstallArriveReq;
import com.bbpms.install.dto.InstallCompleteReq;
import com.bbpms.install.dto.InstallInfoReq;
import com.bbpms.install.dto.InstallPageReq;
import com.bbpms.install.dto.InstallPhotoReq;
import com.bbpms.install.dto.InstallSignatureReq;
import com.bbpms.install.entity.InstallRecord;
import com.bbpms.install.vo.InstallProgressVO;
import com.bbpms.install.vo.InstallRecordVO;

import java.util.List;

public interface InstallService {

    void arrive(Long workOrderId, InstallArriveReq req, Long installerId);

    void saveInfo(Long workOrderId, InstallInfoReq req);

    void addPhoto(Long workOrderId, InstallPhotoReq req);

    void addSignature(Long workOrderId, InstallSignatureReq req);

    /**
     * Saga replacement. Completes the install, calls BSS, marks the order
     * finished, and emits downstream events. On BSS failure it reverts the
     * work order back to IN_PROGRESS and rethrows.
     */
    void complete(Long workOrderId, InstallCompleteReq req, Long installerId);

    InstallRecordVO getByWorkOrderId(Long workOrderId);

    PageResp<InstallRecordVO> page(InstallPageReq req);

    List<InstallRecordVO> getByInstaller(Long installerId);

    InstallProgressVO getProgress(Long workOrderId);

    /** Internal: idempotent record creation, invoked from event listener. */
    void initRecord(Long workOrderId, Long installerId);
}