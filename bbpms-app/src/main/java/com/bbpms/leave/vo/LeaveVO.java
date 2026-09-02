package com.bbpms.leave.vo;

import com.bbpms.leave.entity.LeaveRequest;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "请假视图")
public class LeaveVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long applicantId;
    private String applicantName;
    private String leaveType;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime startAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime endAt;
    private BigDecimal totalHours;
    private String reason;
    private String attachmentUrl;
    private String status;
    private Integer currentLevel;
    private Integer requiredLevel;
    private Long level1ApproverId;
    private String level1ApproverName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime level1DecidedAt;
    private String level1Remark;
    private Long level2ApproverId;
    private String level2ApproverName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime level2DecidedAt;
    private String level2Remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime appliedAt;

    public static LeaveVO from(LeaveRequest r) {
        if (r == null) return null;
        LeaveVO vo = new LeaveVO();
        BeanUtils.copyProperties(r, vo);
        return vo;
    }
}
