package com.bbpms.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Customer master record. Sensitive fields (name, idCardNo, phone) are
 * SM4-encrypted before persistence and masked on read unless the caller has
 * the {@code customer:view-sensitive} permission.
 *
 * <p>Logical delete uses the {@code deleted} column inherited from
 * {@link BaseDO}; no per-class override.</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer")
public class Customer extends BaseDO {

    /** ID card number (UK, stored as ciphertext). */
    private String idCardNo;

    /** Customer name (ciphertext). */
    private String name;

    /** Mobile phone (ciphertext). */
    private String phone;

    /** Detailed install address. */
    private String address;

    private String province;
    private String city;
    private String district;

    /** Latitude / longitude of the install address (for dispatch geo). */
    private BigDecimal lat;
    private BigDecimal lng;

    /** Service grid code from address-normalisation service. */
    private String gridCode;

    /** 1=active, 0=disabled. */
    @TableField(value = "status")
    private Integer status;
}
