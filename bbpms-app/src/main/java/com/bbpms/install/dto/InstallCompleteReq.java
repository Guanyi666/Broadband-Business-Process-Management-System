package com.bbpms.install.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class InstallCompleteReq {
    private Long workOrderId;
    private Long orderId;
    private InstallInfoReq info;
    private List<InstallPhotoReq> photos;
    private InstallSignatureReq signature;
    private BigDecimal lat;
    private BigDecimal lng;
    private BigDecimal distance;
    private String remark;
}