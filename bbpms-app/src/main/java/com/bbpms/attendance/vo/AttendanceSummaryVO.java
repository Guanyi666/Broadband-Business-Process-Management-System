package com.bbpms.attendance.vo;

import com.bbpms.attendance.entity.AttendanceSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "考勤月度汇总")
public class AttendanceSummaryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long installerId;
    private String installerName;
    private String yearMonth;
    private Integer totalWorkMinutes;
    private Integer workDays;
    private Integer lateCount;
    private Integer earlyLeaveCount;
    private Integer absentCount;

    public static AttendanceSummaryVO from(AttendanceSummary s) {
        if (s == null) return null;
        AttendanceSummaryVO vo = new AttendanceSummaryVO();
        BeanUtils.copyProperties(s, vo);
        return vo;
    }
}
