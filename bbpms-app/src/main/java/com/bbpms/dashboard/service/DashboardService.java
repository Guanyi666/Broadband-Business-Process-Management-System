package com.bbpms.dashboard.service;

import com.bbpms.common.enums.OrderStatus;
import com.bbpms.common.enums.WorkOrderStatus;
import com.bbpms.dashboard.mapper.DashboardMapper;
import com.bbpms.dashboard.vo.DashboardDispatchVO;
import com.bbpms.dashboard.vo.DashboardOverviewVO;
import com.bbpms.dashboard.vo.DashboardTrendVO;
import com.bbpms.dashboard.vo.DurationBucketVO;
import com.bbpms.dashboard.vo.KpiItemVO;
import com.bbpms.dashboard.vo.StatusDistItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Dashboard 只读聚合服务（T02）。
 *
 * 约定：
 *  - 只读，不写任何业务表
 *  - 存量指标不伪造环比；昨日为 0 时环比返回 null
 *  - 状态分布 / 趋势 / 分桶全部「全量 + 补 0」，不返回残缺序列
 *  - SQL 兼容 MySQL 默认 ONLY_FULL_GROUP_BY（分组列 = SELECT 列）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DashboardMapper dashboardMapper;

    /** days 归一化：null/<=0/>90 一律回退 7，其余钳制到 [1,90] */
    private int normalizeDays(Integer days) {
        if (days == null || days <= 0 || days > 90) return 7;
        return days;
    }

    private String startOfWindow(int days) {
        return LocalDate.now().minusDays((long) days - 1).format(DATE_FMT);
    }

    // ==================== ① 总览 ====================

    public DashboardOverviewVO overview(Integer daysParam) {
        int days = normalizeDays(daysParam);

        List<KpiItemVO> kpis = new ArrayList<>(6);
        // 流量指标（今日 vs 昨日；昨日=0 时环比 null）
        kpis.add(flowKpi("todayOrders",
                dashboardMapper.countTodayOrders(), dashboardMapper.countYesterdayOrders(), "单"));
        kpis.add(flowKpi("todayFinishedOrders",
                dashboardMapper.countTodayFinishedOrders(), dashboardMapper.countYesterdayFinishedOrders(), "单"));
        kpis.add(flowKpi("todayFinishedWorkorders",
                dashboardMapper.countTodayFinishedWorkorders(), dashboardMapper.countYesterdayFinishedWorkorders(), "个"));
        // 存量指标（无环比基准）
        kpis.add(stockKpi("pendingAuditOrders", dashboardMapper.countPendingAuditOrders(), "单"));
        kpis.add(stockKpi("runningWorkorders", dashboardMapper.countRunningWorkorders(), "个"));
        kpis.add(stockKpi("stalledWorkorders", dashboardMapper.countStalledWorkorders(), "个"));

        return new DashboardOverviewVO(
                LocalDateTime.now().format(DATE_TIME_FMT),
                days,
                kpis,
                fillOrderDist(dashboardMapper.orderStatusDistribution()),
                fillWorkOrderDist(dashboardMapper.workOrderStatusDistribution()));
    }

    /** 流量 KPI：value + prev + delta（prev=0 时 delta=null，不伪造对比基准） */
    private KpiItemVO flowKpi(String key, long today, long yesterday, String unit) {
        Double delta = null;
        if (yesterday > 0) {
            delta = Math.round((today - yesterday) * 1000.0 / yesterday) / 10.0;
        }
        return new KpiItemVO(key, today, yesterday, delta, unit);
    }

    /** 存量 KPI：prev/delta 恒为 null */
    private KpiItemVO stockKpi(String key, long count, String unit) {
        return new KpiItemVO(key, count, null, null, unit);
    }

    /** 以枚举全序为基准补全状态分布（DB 缺失补 0） */
    private List<StatusDistItemVO> fillOrderDist(List<StatusDistItemVO> raw) {
        return fillDist(raw, OrderStatus.values(),
                (code) -> {
                    OrderStatus s = OrderStatus.fromCode(code);
                    return s != null ? s.getDesc() : code;
                });
    }

    private List<StatusDistItemVO> fillWorkOrderDist(List<StatusDistItemVO> raw) {
        return fillDist(raw, WorkOrderStatus.values(),
                (code) -> {
                    WorkOrderStatus s = WorkOrderStatus.fromCode(code);
                    return s != null ? s.getDesc() : code;
                });
    }

    private List<StatusDistItemVO> fillDist(List<StatusDistItemVO> raw,
                                            Enum<?>[] canonical,
                                            Function<String, String> descFn) {
        Map<String, Long> byStatus = new HashMap<>();
        for (StatusDistItemVO item : raw) {
            if (item.getStatus() != null) {
                byStatus.put(item.getStatus(), item.getCount() == null ? 0L : item.getCount());
            }
        }
        List<StatusDistItemVO> result = new ArrayList<>(canonical.length);
        for (Enum<?> e : canonical) {
            String code = e.name();
            result.add(new StatusDistItemVO(code, descFn.apply(code), byStatus.getOrDefault(code, 0L)));
        }
        return result;
    }

    // ==================== ② 趋势 ====================

    public DashboardTrendVO trend(Integer daysParam) {
        int days = normalizeDays(daysParam);
        String start = startOfWindow(days);

        // 完整日期轴 [start, 今日]
        List<String> dates = new ArrayList<>(days);
        LocalDate base = LocalDate.now().minusDays((long) days - 1);
        for (int i = 0; i < days; i++) {
            dates.add(base.plusDays(i).format(DATE_FMT));
        }

        Map<String, Long> createdByDay = toDayMap(dashboardMapper.orderCreatedTrend(start));
        Map<String, Long> finishedByDay = toDayMap(dashboardMapper.workOrderFinishedTrend(start));

        List<Long> createdCounts = new ArrayList<>(days);
        List<Long> finishedCounts = new ArrayList<>(days);
        for (String d : dates) {
            createdCounts.add(createdByDay.getOrDefault(d, 0L));
            finishedCounts.add(finishedByDay.getOrDefault(d, 0L));
        }
        return new DashboardTrendVO(dates, createdCounts, finishedCounts);
    }

    /** 按 yyyy-MM-dd 聚合日计数 */
    private Map<String, Long> toDayMap(List<DashboardMapper.DayCount> raw) {
        Map<String, Long> map = new HashMap<>();
        for (DashboardMapper.DayCount item : raw) {
            if (item.getDate() != null && item.getCount() != null) {
                map.put(item.getDate(), item.getCount());
            }
        }
        return map;
    }

    // ==================== ③ 派单时效分桶 ====================

    public DashboardDispatchVO dispatchDuration(Integer daysParam) {
        int days = normalizeDays(daysParam);
        String start = startOfWindow(days);

        Map<String, Long> buckets = new LinkedHashMap<>();
        buckets.put("LT_30M", 0L);
        buckets.put("M30_60", 0L);
        buckets.put("H1_2", 0L);
        buckets.put("H2_4", 0L);
        buckets.put("GT_4H", 0L);

        for (Long minutes : dashboardMapper.listDispatchDurations(start)) {
            if (minutes == null) continue;
            String code;
            if (minutes < 30) code = "LT_30M";
            else if (minutes < 60) code = "M30_60";
            else if (minutes < 120) code = "H1_2";
            else if (minutes < 240) code = "H2_4";
            else code = "GT_4H";
            buckets.put(code, buckets.get(code) + 1L);
        }

        List<DurationBucketVO> result = new ArrayList<>(5);
        result.add(new DurationBucketVO("LT_30M", "<30 分钟", buckets.get("LT_30M")));
        result.add(new DurationBucketVO("M30_60", "30-60 分钟", buckets.get("M30_60")));
        result.add(new DurationBucketVO("H1_2", "1-2 小时", buckets.get("H1_2")));
        result.add(new DurationBucketVO("H2_4", "2-4 小时", buckets.get("H2_4")));
        result.add(new DurationBucketVO("GT_4H", ">4 小时", buckets.get("GT_4H")));

        return new DashboardDispatchVO(days, result);
    }
}
