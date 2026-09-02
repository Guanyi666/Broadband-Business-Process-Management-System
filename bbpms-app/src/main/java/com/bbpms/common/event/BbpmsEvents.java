package com.bbpms.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Internal event definitions (replacing RabbitMQ + Outbox).
 *
 * <p>Used with Spring {@code ApplicationEventPublisher} and
 * {@code @TransactionalEventListener(AFTER_COMMIT)} to preserve
 * the reliable-event semantics of the original MQ/Outbox design
 * without the operational overhead.
 */
public final class BbpmsEvents {

    private BbpmsEvents() {}

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderCreatedEvent {
        private Long orderId;
        private String orderNo;
        private Long customerId;
        private String packageCode;
        private Long csId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderAuditedEvent {
        private Long orderId;
        private String orderNo;
        private Long auditorId;
        private LocalDateTime auditTime;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderCancelledEvent {
        private Long orderId;
        private Long operatorId;
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkOrderDispatchedEvent {
        private Long workOrderId;
        private String workNo;
        private Long orderId;
        private Long installerId;
        private Double score;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkOrderAcceptedEvent {
        private Long workOrderId;
        private Long installerId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkOrderCompletedEvent {
        private Long workOrderId;
        private Long installerId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstallCompletedEvent {
        private Long workOrderId;
        private Long orderId;
        private Long installerId;
        private String onuMac;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkOrderTransferEvent {
        private Long workOrderId;
        private Long installerId;
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkOrderDispatchFailedEvent {
        private Long workOrderId;
        private Long installerId;
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotifyEvent {
        /** Channel: SMS / WECHAT / INAPP */
        private String channel;
        private String phone;
        private String openId;
        private Long userId;
        private String templateCode;
        private java.util.Map<String, Object> params;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationLogEvent {
        private Long userId;
        private String username;
        private String module;
        private String action;
        private String requestUri;
        private String method;
        private String params;
        private String ip;
        private String userAgent;
        private Long costMs;
        private Integer status;
        private String error;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginLogEvent {
        private Long userId;
        private String username;
        private String ip;
        private String userAgent;
        private Integer status;
        private String message;
    }

    // -------- Attendance (Phase 3) --------

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstallerClockInEvent {
        private Long installerId;
        private java.time.LocalDateTime timestamp;
        private java.math.BigDecimal lat;
        private java.math.BigDecimal lng;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstallerClockOutEvent {
        private Long installerId;
        private java.time.LocalDateTime timestamp;
        private Integer workMinutes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstallerBreakEvent {
        private Long installerId;
        /** "START" or "END" */
        private String type;
        private java.time.LocalDateTime timestamp;
    }

    // -------- Leave (Phase 4) --------

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaveAppliedEvent {
        private Long leaveId;
        private Long applicantId;
        private String leaveType;
        private java.time.LocalDateTime startAt;
        private java.time.LocalDateTime endAt;
        private Integer requiredLevel;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaveApprovedEvent {
        private Long leaveId;
        private Long applicantId;
        private String leaveType;
        private java.time.LocalDateTime startAt;
        private java.time.LocalDateTime endAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaveRejectedEvent {
        private Long leaveId;
        private Long applicantId;
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaveCancelledEvent {
        private Long leaveId;
        private Long applicantId;
    }

    // -------- Work-order lifecycle (Phase 5) --------

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkOrderStalledEvent {
        private Long workOrderId;
        private Long installerId;
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkOrderResumedEvent {
        private Long workOrderId;
        private Long installerId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkOrderAutoCancelledEvent {
        private Long workOrderId;
        private Long originalInstallerId;
        private String reason;
        private String cancelType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkOrderReassignedEvent {
        private Long workOrderId;
        private Long fromInstallerId;
        private Long toInstallerId;
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkOrderForceClosedEvent {
        private Long workOrderId;
        private Long operatorId;
        private String reason;
        private String cancelType;
    }
}
