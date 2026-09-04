package com.bbpms.customerportal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_user_binding")
public class CustomerUserBinding extends BaseDO {
    private Long userId;
    private Long customerId;
    private Integer status;
    private LocalDateTime bindTime;
}
