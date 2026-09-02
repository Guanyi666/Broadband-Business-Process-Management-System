package com.bbpms.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bbpms.attendance.dto.AttendanceMonthlyStat;
import com.bbpms.attendance.entity.AttendanceRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AttendanceRecordMapper extends BaseMapper<AttendanceRecord> {

    @Select("SELECT * FROM att_attendance_record WHERE installer_id = #{installerId} AND work_date = #{workDate} AND deleted = 0 LIMIT 1")
    AttendanceRecord findByInstallerAndDate(@Param("installerId") Long installerId,
                                             @Param("workDate") LocalDate workDate);

    @Select("SELECT * FROM att_attendance_record WHERE installer_id = #{installerId} AND deleted = 0 ORDER BY work_date DESC LIMIT #{limit}")
    List<AttendanceRecord> findRecent(@Param("installerId") Long installerId,
                                      @Param("limit") int limit);

    @Select("""
            SELECT * FROM att_attendance_record
            WHERE status IN ('ON_DUTY', 'ON_BREAK')
              AND clock_in_at IS NOT NULL
              AND clock_out_at IS NULL
              AND deleted = 0
              AND clock_in_at < #{cutoff}
            """)
    List<AttendanceRecord> findStaleOnDuty(@Param("cutoff") java.time.LocalDateTime cutoff);

    @Update("UPDATE att_attendance_record SET status = 'AUTO_OFF', clock_out_at = #{now}, work_minutes = TIMESTAMPDIFF(MINUTE, clock_in_at, #{now}) - IFNULL(break_minutes, 0), update_time = NOW() WHERE id = #{id} AND deleted = 0")
    int autoClockOut(@Param("id") Long id, @Param("now") java.time.LocalDateTime now);

    @Update("DELETE FROM att_attendance_record WHERE work_date < #{cutoff} AND deleted = 0")
    int purgeBefore(@Param("cutoff") LocalDate cutoff);

    /**
     * SA-P2-001: monthly rollup over one installer's daily records.
     * work_days counts signed-in days; late = clock-in past 09:00+threshold;
     * early_leave = clock-out before {@code offDutyTime}. Absent days are
     * computed in Java (weekdays elapsed in the month minus work days).
     */
    @Select("""
            SELECT COALESCE(SUM(work_minutes), 0)  AS total_work_minutes,
                   COUNT(*)                        AS work_days,
                   COALESCE(SUM(CASE WHEN TIME(clock_in_at) > SEC_TO_TIME(9 * 3600 + #{lateThresholdMinutes} * 60)
                                     THEN 1 ELSE 0 END), 0) AS late_count,
                   COALESCE(SUM(CASE WHEN clock_out_at IS NOT NULL
                                      AND TIME(clock_out_at) < #{offDutyTime}
                                     THEN 1 ELSE 0 END), 0) AS early_leave_count
            FROM att_attendance_record
            WHERE installer_id = #{installerId}
              AND work_date BETWEEN #{monthStart} AND #{monthEnd}
              AND clock_in_at IS NOT NULL
              AND deleted = 0
            """)
    AttendanceMonthlyStat aggregateMonth(@Param("installerId") Long installerId,
                                         @Param("monthStart") LocalDate monthStart,
                                         @Param("monthEnd") LocalDate monthEnd,
                                         @Param("lateThresholdMinutes") int lateThresholdMinutes,
                                         @Param("offDutyTime") String offDutyTime);
}
