package com.bbpms.order.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Customer view object.
 *
 * <p>When the caller does NOT have the {@code customer:view-sensitive}
 * permission, {@code name}, {@code idCardNo} and {@code phone} are returned
 * in masked form (e.g. {@code 138****0001}).</p>
 */
@Data
@Schema(description = "客户视图")
public class CustomerVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "客户姓名（已脱敏）")
    private String name;

    @Schema(description = "手机号（已脱敏）")
    private String phone;

    @Schema(description = "身份证号（已脱敏）")
    private String idCardNo;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "省")
    private String province;

    @Schema(description = "市")
    private String city;

    @Schema(description = "区")
    private String district;

    @Schema(description = "纬度")
    private BigDecimal lat;

    @Schema(description = "经度")
    private BigDecimal lng;

    @Schema(description = "网格编码")
    private String gridCode;

    /** {@code true} when sensitive PII fields have been masked on read. */
    @Schema(description = "是否已脱敏")
    private Boolean masked;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
