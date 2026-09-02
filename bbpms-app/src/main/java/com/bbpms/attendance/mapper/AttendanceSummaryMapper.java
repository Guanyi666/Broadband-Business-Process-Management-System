package com.bbpms.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bbpms.attendance.entity.AttendanceSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AttendanceSummaryMapper extends BaseMapper<AttendanceSummary> {

    // `year_month` is a MySQL 8 reserved word — must be backticked (SA-P2-001).
    @Select("SELECT * FROM att_attendance_summary WHERE installer_id = #{installerId} AND `year_month` = #{yearMonth} AND deleted = 0 LIMIT 1")
    AttendanceSummary findByInstallerAndMonth(@Param("installerId") Long installerId,
                                               @Param("yearMonth") String yearMonth);
}
