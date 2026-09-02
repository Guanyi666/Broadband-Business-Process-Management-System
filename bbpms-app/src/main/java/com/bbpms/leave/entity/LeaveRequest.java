package com.bbpms.leave.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lv_leave_request")
public class LeaveRequest extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long applicantId;
    /** CASUAL | ANNUAL | SICK | COMPASSIONATE | UNPAID */
    private String leaveType;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private BigDecimal totalHours;
    private String reason;
    private String attachmentUrl;
    /** PENDING | APPROVED | REJECTED | CANCELLED */
    private String status;
    private Integer currentLevel;
    private Integer requiredLevel;
    private Long level1ApproverId;
    private LocalDateTime level1DecidedAt;
    private String level1Remark;
    private Long level2ApproverId;
    private LocalDateTime level2DecidedAt;
    private String level2Remark;
    private LocalDateTime appliedAt;
}
