package com.bbpms.install.dto;

import lombok.Data;

@Data
public class InstallPhotoReq {
    private Long workOrderId;
    private String objectKey;
    private String url;
}