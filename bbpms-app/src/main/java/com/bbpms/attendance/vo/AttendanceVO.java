package com.bbpms.attendance.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.bbpms.attendance.entity.AttendanceRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "考勤视图")
public class AttendanceVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long installerId;
    private String installerName;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate workDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime clockInAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime clockOutAt;
    private Integer workMinutes;
    private Integer breakMinutes;
    private LocalDateTime breakStartAt;
    /** OFF_DUTY | ON_DUTY | ON_BREAK | AUTO_OFF */
    private String status;
    private String source;
    private BigDecimal locationLat;
    private BigDecimal locationLng;
    private String remark;
    private Boolean isLate;
    private Boolean isEarlyLeave;

    public static AttendanceVO from(AttendanceRecord r) {
        if (r == null) return null;
        AttendanceVO vo = new AttendanceVO();
        BeanUtils.copyProperties(r, vo);
        return vo;
    }
}
