package com.bbpms.install.bss;

import com.bbpms.install.dto.BssActivateReq;
import com.bbpms.install.dto.BssActivateResp;

/**
 * BSS (Business Support System) activation abstraction. Real implementations
 * would call the operator's provisioning API; in the monolith a mock
 * implementation stands in so the rest of the saga can be exercised.
 */
public interface BssClient {

    /**
     * Activate the broadband service for the given order.
     *
     * @throws com.bbpms.common.exception.BizException on any failure that
     *         should be surfaced to the caller (which triggers compensating
     *         actions in the saga-replacement flow).
     */
    BssActivateResp activate(BssActivateReq req);
}