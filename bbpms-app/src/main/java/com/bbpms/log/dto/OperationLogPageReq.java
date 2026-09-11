package com.bbpms.log.dto;
import com.bbpms.common.entity.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper = true)
public class OperationLogPageReq extends BaseDTO {
    private Long userId;
    private String module;
    private String action;
    /** 1=SUCCESS, 0=FAILED. */
    private Integer status;
}
