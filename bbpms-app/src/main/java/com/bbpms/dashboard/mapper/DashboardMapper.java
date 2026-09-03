package com.bbpms.dashboard.mapper;

import com.bbpms.dashboard.vo.EfficiencyBucketVO;
import com.bbpms.dashboard.vo.StatusSliceVO;
import com.bbpms.dashboard.vo.TrendPointVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DashboardMapper {

    @Select("SELECT COUNT(*) FROM broadband_order WHERE deleted = 0 AND create_time >= CURDATE()")
    long countTodayOrders();

    @Select("SELECT COUNT(*) FROM broadband_order WHERE deleted = 0 AND status = 'CREATED'")
    long countPendingAudit();

    @Select("""
            SELECT COUNT(*) FROM work_order
            WHERE deleted = 0 AND status IN ('DISPATCHED','ACCEPTED','IN_PROGRESS','STALLED','REASSIGNING')
            """)
    long countRunningWorkorders();

    @Select("""
            SELECT COUNT(*) FROM work_order
            WHERE deleted = 0 AND status = 'COMPLETED' AND finish_time >= CURDATE()
            """)
    long countCompletedToday();

    @Select("""
            SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS `date`, COUNT(*) AS `count`
            FROM broadband_order
            WHERE deleted = 0 AND create_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY)
            GROUP BY DATE(create_time)
            ORDER BY DATE(create_time)
            """)
    List<TrendPointVO> orderTrend(@Param("days") int days);

    @Select("""
            SELECT status, COUNT(*) AS `count`
            FROM broadband_order
            WHERE deleted = 0
            GROUP BY status
            ORDER BY FIELD(status, 'CREATED','AUDITED','WAIT_DISPATCH','DISPATCHED','INSTALLING','FINISHED','CLOSED','CANCELLED')
            """)
    List<StatusSliceVO> orderStatusDistribution();

    @Select("""
            SELECT bucket, COUNT(*) AS `count`
            FROM (
              SELECT CASE
                WHEN TIMESTAMPDIFF(MINUTE, create_time, dispatch_time) < 30 THEN '<30m'
                WHEN TIMESTAMPDIFF(MINUTE, create_time, dispatch_time) < 60 THEN '30-60m'
                WHEN TIMESTAMPDIFF(MINUTE, create_time, dispatch_time) < 120 THEN '1-2h'
                WHEN TIMESTAMPDIFF(MINUTE, create_time, dispatch_time) < 240 THEN '2-4h'
                ELSE '>4h'
              END AS bucket,
              CASE
                WHEN TIMESTAMPDIFF(MINUTE, create_time, dispatch_time) < 30 THEN 1
                WHEN TIMESTAMPDIFF(MINUTE, create_time, dispatch_time) < 60 THEN 2
                WHEN TIMESTAMPDIFF(MINUTE, create_time, dispatch_time) < 120 THEN 3
                WHEN TIMESTAMPDIFF(MINUTE, create_time, dispatch_time) < 240 THEN 4
                ELSE 5
              END AS bucket_order
              FROM work_order
              WHERE deleted = 0 AND dispatch_time IS NOT NULL
            ) t
            GROUP BY bucket, bucket_order
            ORDER BY bucket_order
            """)
    List<EfficiencyBucketVO> dispatchEfficiency();
}
