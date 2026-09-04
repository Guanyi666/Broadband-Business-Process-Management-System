package com.bbpms.customerportal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_profile_change")
public class CustomerProfileChange extends BaseDO {
    private Long customerId;
    private Long applicantUserId;
    private String changeFields;
    private String proposedName;
    private String proposedPhone;
    private String proposedIdCardNo;
    private String proposedAddress;
    private String status;
    private Long reviewerId;
    private String reviewRemark;
    private LocalDateTime reviewTime;
}
