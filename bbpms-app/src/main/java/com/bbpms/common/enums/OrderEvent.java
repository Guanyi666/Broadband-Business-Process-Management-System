package com.bbpms.common.enums;

/** Order lifecycle events. RESUBMIT: REJECTED -> CREATED (customer-service edits and resubmits a rejected order). */
public enum OrderEvent { CREATE, AUDIT_PASS, AUDIT_REJECT, RESUBMIT, CANCEL, START_DISPATCH, DISPATCH_OK, ACCEPT, TRANSFER, DISPATCH_TIMEOUT, COMPLETE, CONFIRM }
