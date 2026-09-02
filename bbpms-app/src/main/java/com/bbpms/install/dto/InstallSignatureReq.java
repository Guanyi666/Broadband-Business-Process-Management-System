package com.bbpms.install.dto;

import lombok.Data;

@Data
public class InstallSignatureReq {
    private Long workOrderId;
    private String customerName;
    private String dataUrl;
    private String objectKey;
}