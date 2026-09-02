package com.bbpms.leave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bbpms.leave.entity.LeaveRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LeaveRequestMapper extends BaseMapper<LeaveRequest> {

    @Select("""
            SELECT * FROM lv_leave_request
            WHERE applicant_id = #{applicantId}
              AND status = 'APPROVED'
              AND start_at <= #{now}
              AND end_at >= #{now}
              AND deleted = 0
            LIMIT 1
            """)
    LeaveRequest findActiveLeave(@Param("applicantId") Long applicantId,
                                 @Param("now") LocalDateTime now);

    @Select("""
            SELECT * FROM lv_leave_request
            WHERE status = 'APPROVED'
              AND start_at <= #{now}
              AND end_at >= #{now}
              AND deleted = 0
            """)
    List<LeaveRequest> findAllActive(@Param("now") LocalDateTime now);
}
