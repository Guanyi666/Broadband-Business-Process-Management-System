package com.bbpms.customerportal.dto;

import com.bbpms.order.vo.CustomerVO;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** DTO collection for the customer portal and its admin console. */
public final class PortalDtos {
    private PortalDtos() {}

    @Data
    public static class AccountCreateReq {
        @NotNull private Long customerId;
        @NotBlank @Size(min = 4, max = 64) private String username;
        @NotBlank @Size(min = 6, max = 64) private String password;
    }

    @Data
    public static class PasswordResetReq {
        @NotBlank @Size(min = 6, max = 64) private String newPassword;
    }

    @Data
    public static class PasswordChangeReq {
        @NotBlank private String oldPassword;
        @NotBlank @Size(min = 6, max = 64) private String newPassword;
    }

    @Data
    public static class AccountStatusReq {
        @NotNull private Integer status;
    }

    @Data
    public static class AccountVO {
        private Long customerId;
        private Long userId;
        private String username;
        private String nickname;
        private Integer status;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime bindTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime lastLoginTime;
    }

    @Data
    public static class PackageVO {
        private Long id;
        private String code;
        private String name;
        private Integer speedMbps;
        private BigDecimal monthlyFee;
        private String description;
    }

    @Data
    public static class CustomerOrderCreateReq {
        @NotBlank private String packageCode;
        @NotBlank @Size(max = 512) private String installAddress;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime appointmentTime;
        @Size(max = 32) private String contactPhone;
        @Size(max = 255) private String remark;
        private String roomNo;
    }

    @Data
    public static class CustomerOrderSummaryVO {
        private Long id;
        private String orderNo;
        private String packageCode;
        private String packageName;
        private String installAddress;
        private String status;
        private String statusLabel;
        private String resourceStatus;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime appointmentTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime completedTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createTime;
        private Boolean canResubmit;
        private Boolean canReschedule;
        private Boolean canEvaluate;
    }

    @Data
    public static class CustomerOrderResubmitReq {
        @NotBlank @Size(max = 512) private String installAddress;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime appointmentTime;
        @Size(max = 255) private String remark;
    }

    @Data
    public static class OrderReviewReq {
        @NotNull private Boolean approved;
        @Size(max = 512) private String remark;
    }

    @Data
    public static class AppointmentChangeReq {
        @NotNull @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime appointmentTime;
        @Size(max = 512) private String reason;
    }

    @Data
    public static class TicketCreateReq {
        @NotBlank private String type;
        private Long orderId;
        @Size(max = 64) private String category;
        @NotBlank @Size(max = 1000) private String description;
        private List<String> attachments;
    }

    @Data
    public static class TicketHandleReq {
        @NotBlank private String status;
        private Long handlerId;
        @Size(max = 1000) private String handleResult;
    }

    @Data
    public static class EvaluationCreateReq {
        @NotNull @Min(1) @Max(5) private Integer overallScore;
        @Min(1) @Max(5) private Integer serviceScore;
        @Min(1) @Max(5) private Integer qualityScore;
        @Min(1) @Max(5) private Integer punctualityScore;
        private List<String> tags;
        @Size(max = 1000) private String content;
    }

    @Data
    public static class ProfileChangeReq {
        @Size(max = 64) private String name;
        @Size(max = 32) private String phone;
        @Size(max = 32) private String idCardNo;
        @Size(max = 512) private String address;
    }

    @Data
    public static class ProfileReviewReq {
        @NotNull private Boolean approved;
        @Size(max = 512) private String remark;
    }

    @Data
    public static class ProfileChangeVO {
        private Long id;
        private Long customerId;
        private String changeFields;
        private String proposedName;
        private String proposedPhone;
        private String proposedIdCardNo;
        private String proposedAddress;
        private String status;
        private Long reviewerId;
        private String reviewRemark;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime reviewTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createTime;
    }

    @Data
    public static class ProfileVO {
        private AccountVO account;
        private CustomerVO customer;
    }
}
