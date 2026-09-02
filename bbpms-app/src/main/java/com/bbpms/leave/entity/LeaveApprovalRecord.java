package com.bbpms.leave.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lv_leave_approval_record")
public class LeaveApprovalRecord extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long leaveId;
    private Long approverId;
    private Integer approvalLevel;
    /** APPROVED | REJECTED | ESCALATED */
    private String action;
    private String comment;
}
