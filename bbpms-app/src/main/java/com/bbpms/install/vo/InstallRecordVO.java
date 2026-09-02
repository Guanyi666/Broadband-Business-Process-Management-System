package com.bbpms.install.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class InstallRecordVO {
    private Long id;
    private Long workOrderId;
    private Long installerId;
    private String onuMac;
    private String onuSn;
    private String oltPort;
    private BigDecimal signalDb;
    private BigDecimal completeLat;
    private BigDecimal completeLng;
    private Integer photoCount;
    private String signatureUrl;
    private String customerSignatureName;
    private String remark;
    private String status;
    private LocalDateTime submitTime;
    private List<String> photos = new ArrayList<>();
}