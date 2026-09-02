package com.bbpms.install.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("install_record")
public class InstallRecord extends BaseDO {
    private static final long serialVersionUID = 1L;

    @TableField("work_order_id")
    private Long workOrderId;

    @TableField("installer_id")
    private Long installerId;

    @TableField("onu_mac")
    private String onuMac;

    @TableField("onu_sn")
    private String onuSn;

    @TableField("olt_port")
    private String oltPort;

    @TableField("signal_db")
    private BigDecimal signalDb;

    @TableField("start_lat")
    private BigDecimal startLat;

    @TableField("start_lng")
    private BigDecimal startLng;

    @TableField("complete_lat")
    private BigDecimal completeLat;

    @TableField("complete_lng")
    private BigDecimal completeLng;

    /** JSON array of object keys / urls. */
    @TableField("photos")
    private String photos;

    @TableField("signature_url")
    private String signatureUrl;

    @TableField("customer_signature_name")
    private String customerSignatureName;

    @TableField("remark")
    private String remark;

    @TableField("submit_time")
    private LocalDateTime submitTime;

    /** Status string: PENDING / IN_PROGRESS / COMPLETED / FAILED. */
    @TableField("status")
    private String status;
}