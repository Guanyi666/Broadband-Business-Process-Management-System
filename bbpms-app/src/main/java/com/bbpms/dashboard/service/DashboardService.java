package com.bbpms.dashboard.service;

import com.bbpms.dashboard.mapper.DashboardMapper;
import com.bbpms.dashboard.vo.DashboardKpiVO;
import com.bbpms.dashboard.vo.EfficiencyBucketVO;
import com.bbpms.dashboard.vo.StatusSliceVO;
import com.bbpms.dashboard.vo.TrendPointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private final DashboardMapper dashboardMapper;

    public DashboardKpiVO kpi() {
        return new DashboardKpiVO(
                dashboardMapper.countTodayOrders(),
                dashboardMapper.countPendingAudit(),
                dashboardMapper.countRunningWorkorders(),
                dashboardMapper.countCompletedToday());
    }

    public List<TrendPointVO> orderTrend(int requestedDays) {
        int days = Math.max(1, Math.min(requestedDays, 90));
        Map<String, Long> counts = dashboardMapper.orderTrend(days).stream()
                .collect(Collectors.toMap(TrendPointVO::getDate, TrendPointVO::getCount));
        List<TrendPointVO> result = new ArrayList<>(days);
        LocalDate start = LocalDate.now().minusDays(days - 1L);
        for (int i = 0; i < days; i++) {
            String date = start.plusDays(i).format(DATE_FORMAT);
            TrendPointVO point = new TrendPointVO();
            point.setDate(date);
            point.setCount(counts.getOrDefault(date, 0L));
            result.add(point);
        }
        return result;
    }

    public List<StatusSliceVO> orderStatusDistribution() {
        return dashboardMapper.orderStatusDistribution();
    }

    public List<EfficiencyBucketVO> dispatchEfficiency() {
        return dashboardMapper.dispatchEfficiency();
    }
}
