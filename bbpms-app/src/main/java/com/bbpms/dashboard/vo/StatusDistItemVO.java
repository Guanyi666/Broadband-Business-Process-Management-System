package com.bbpms.dashboard.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 状态分布项（订单 8 态 / 工单 10 态，存量全量口径）
 */
@Data
@NoArgsConstructor
public class StatusDistItemVO {

    /** 状态编码，如 CREATED / DISPATCHED */
    private String status;

    /** 状态中文名（取自已核对过的后端枚举） */
    private String statusDesc;

    /** 该状态下存量数量 */
    private Long count;

    public StatusDistItemVO(String status, String statusDesc, Long count) {
        this.status = status;
        this.statusDesc = statusDesc;
        this.count = count;
    }
}
