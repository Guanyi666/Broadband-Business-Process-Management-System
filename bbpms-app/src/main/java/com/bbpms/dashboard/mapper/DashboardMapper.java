package com.bbpms.dashboard.mapper;

import com.bbpms.dashboard.vo.StatusDistItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Dashboard 只读统计 Mapper（T02）。
 *
 * 只读约定：
 *  - 全部为 SELECT，不触碰任何业务数据
 *  - 全部带 deleted = 0（逻辑删除过滤）
 *  - 严格兼容 MySQL 8 默认 sql_mode=ONLY_FULL_GROUP_BY：
 *    分组查询中 SELECT 列表只出现「分组表达式本身」或聚合函数，绝不在分组表达式之外裸选列
 *  - 时间窗起点由服务层以 yyyy-MM-dd 字符串传入（避免 DB 时区差异）
 */
@Mapper
public interface DashboardMapper {

    /** 单日计数行（趋势查询专用：date 为 yyyy-MM-dd） */
    @lombok.Data
    @lombok.NoArgsConstructor
    class DayCount {
        private String date;
        private Long count;
    }

    // ---------- 今日 / 昨日 计数（流量指标） ----------

    /** 今日新增订单：create_time >= 今日 0 点 */
    @Select("SELECT COUNT(*) FROM broadband_order WHERE deleted = 0 AND create_time >= CURDATE()")
    long countTodayOrders();

    /** 昨日新增订单：昨日 0 点 <= create_time < 今日 0 点 */
    @Select("SELECT COUNT(*) FROM broadband_order WHERE deleted = 0 AND create_time >= DATE_SUB(CURDATE(), INTERVAL 1 DAY) AND create_time < CURDATE()")
    long countYesterdayOrders();

    /** 今日完成订单：completed_time >= 今日 0 点 */
    @Select("SELECT COUNT(*) FROM broadband_order WHERE deleted = 0 AND completed_time IS NOT NULL AND completed_time >= CURDATE()")
    long countTodayFinishedOrders();

    /** 昨日完成订单 */
    @Select("SELECT COUNT(*) FROM broadband_order WHERE deleted = 0 AND completed_time IS NOT NULL AND completed_time >= DATE_SUB(CURDATE(), INTERVAL 1 DAY) AND completed_time < CURDATE()")
    long countYesterdayFinishedOrders();

    /** 今日完成工单：work_order status=COMPLETED 且 finish_time >= 今日 0 点 */
    @Select("SELECT COUNT(*) FROM work_order WHERE deleted = 0 AND status = 'COMPLETED' AND finish_time IS NOT NULL AND finish_time >= CURDATE()")
    long countTodayFinishedWorkorders();

    /** 昨日完成工单 */
    @Select("SELECT COUNT(*) FROM work_order WHERE deleted = 0 AND status = 'COMPLETED' AND finish_time IS NOT NULL AND finish_time >= DATE_SUB(CURDATE(), INTERVAL 1 DAY) AND finish_time < CURDATE()")
    long countYesterdayFinishedWorkorders();

    // ---------- 存量指标（无环比基准） ----------

    /** 待审核订单存量：status=CREATED */
    @Select("SELECT COUNT(*) FROM broadband_order WHERE deleted = 0 AND status = 'CREATED'")
    long countPendingAuditOrders();

    /** 进行中工单存量：已派单/已接单/施工中/改派中 */
    @Select("SELECT COUNT(*) FROM work_order WHERE deleted = 0 AND status IN ('DISPATCHED','ACCEPTED','IN_PROGRESS','REASSIGNING')")
    long countRunningWorkorders();

    /** 停滞工单存量：status=STALLED */
    @Select("SELECT COUNT(*) FROM work_order WHERE deleted = 0 AND status = 'STALLED'")
    long countStalledWorkorders();

    // ---------- 趋势（分组表达式 = SELECT 表达式，ONLY_FULL_GROUP_BY 安全） ----------

    /**
     * 每日订单创建量
     * @param startDate 窗口起点（含），yyyy-MM-dd；窗口 = [startDate, 今日]
     */
    @Select("""
            SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS `date`, COUNT(*) AS `count`
            FROM broadband_order
            WHERE deleted = 0 AND create_time >= #{startDate}
            GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d')
            ORDER BY DATE_FORMAT(create_time, '%Y-%m-%d')
            """)
    List<DayCount> orderCreatedTrend(@Param("startDate") String startDate);

    /**
     * 每日工单完成量（finish_time 口径）
     * @param startDate 窗口起点（含），yyyy-MM-dd
     */
    @Select("""
            SELECT DATE_FORMAT(finish_time, '%Y-%m-%d') AS `date`, COUNT(*) AS `count`
            FROM work_order
            WHERE deleted = 0 AND status = 'COMPLETED' AND finish_time IS NOT NULL AND finish_time >= #{startDate}
            GROUP BY DATE_FORMAT(finish_time, '%Y-%m-%d')
            ORDER BY DATE_FORMAT(finish_time, '%Y-%m-%d')
            """)
    List<DayCount> workOrderFinishedTrend(@Param("startDate") String startDate);

    // ---------- 状态分布 ----------

    /** 订单状态存量分布（按状态分组，缺失状态由服务层补 0） */
    @Select("SELECT status, COUNT(*) AS `count` FROM broadband_order WHERE deleted = 0 GROUP BY status")
    List<StatusDistItemVO> orderStatusDistribution();

    /** 工单状态存量分布 */
    @Select("SELECT status, COUNT(*) AS `count` FROM work_order WHERE deleted = 0 GROUP BY status")
    List<StatusDistItemVO> workOrderStatusDistribution();

    // ---------- 派单时效（派单 → 接单 分钟数明细，服务层分桶） ----------

    /**
     * 窗口内已接单工单的派单→接单耗时（分钟）
     * 仅统计 dispatch_time 与 accept_time 均非空的正常记录
     */
    @Select("""
            SELECT TIMESTAMPDIFF(MINUTE, dispatch_time, accept_time) AS `durationMinutes`
            FROM work_order
            WHERE deleted = 0
              AND dispatch_time IS NOT NULL
              AND accept_time IS NOT NULL
              AND dispatch_time >= #{startDate}
            ORDER BY dispatch_time DESC
            LIMIT 5000
            """)
    List<Long> listDispatchDurations(@Param("startDate") String startDate);
}
