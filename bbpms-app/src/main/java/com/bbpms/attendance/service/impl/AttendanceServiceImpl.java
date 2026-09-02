package com.bbpms.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bbpms.attendance.config.AttendanceProperties;
import com.bbpms.attendance.dto.AttendanceMonthlyStat;
import com.bbpms.attendance.dto.BreakReq;
import com.bbpms.attendance.dto.ClockInReq;
import com.bbpms.attendance.dto.ClockOutReq;
import com.bbpms.attendance.entity.AttendanceRecord;
import com.bbpms.attendance.entity.AttendanceSummary;
import com.bbpms.attendance.mapper.AttendanceRecordMapper;
import com.bbpms.attendance.mapper.AttendanceSummaryMapper;
import com.bbpms.attendance.service.AttendanceService;
import com.bbpms.attendance.vo.AttendanceSummaryVO;
import com.bbpms.attendance.vo.AttendanceVO;
import com.bbpms.common.annotation.OperationLog;
import com.bbpms.common.entity.BaseDO;
import com.bbpms.common.event.BbpmsEvents;
import com.bbpms.common.exception.BizException;
import com.bbpms.common.util.RedisUtils;
import com.bbpms.common.util.SecurityUtils;
import com.bbpms.user.entity.InstallerProfile;
import com.bbpms.user.mapper.SysUserMapper;
import com.bbpms.user.service.InstallerProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default implementation. Clock-in is idempotent (returns existing record
 * for the day if already signed in). Both sides of the day are transactional
 * so attendance and installer_profile.on_duty stay consistent.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    public static final String ACTIVE_ZSET = "attendance:active";

    /**
     * Dispatch / online-list source of truth (SA-P1-005): written by
     * InstallerProfileServiceImpl.updateLocation and now also by clock-in/out,
     * read by DispatchServiceImpl.loadCandidates and InstallerProfileService.getOnline.
     */
    public static final String ONLINE_KEY = "installers:active";
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private final AttendanceRecordMapper recordMapper;
    private final AttendanceSummaryMapper summaryMapper;
    private final InstallerProfileService installerProfileService;
    private final SysUserMapper sysUserMapper;
    private final RedisUtils redisUtils;
    private final AttendanceProperties props;
    private final ApplicationEventPublisher publisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(value = "签到", module = "考勤")
    public AttendanceVO clockIn(Long installerId, ClockInReq req) {
        if (installerId == null) {
            throw new BizException(com.bbpms.common.enums.ResultCode.BAD_REQUEST, "installerId 必填");
        }
        LocalDate today = LocalDate.now();

        // Idempotent: return existing record only while it is still OPEN (no clock-out).
        // A same-day re-sign-in after clock-out starts a new duty period (SB-N1).
        AttendanceRecord existing = recordMapper.findByInstallerAndDate(installerId, today);
        if (existing != null && existing.getClockInAt() != null && existing.getClockOutAt() == null) {
            log.info("clockIn: installer {} already signed in at {}", installerId, existing.getClockInAt());
            return enrich(existing, installerId);
        }
        boolean reSignIn = existing != null && existing.getClockOutAt() != null;

        AttendanceRecord rec = existing == null ? new AttendanceRecord() : existing;
        rec.setInstallerId(installerId);
        rec.setWorkDate(today);
        rec.setClockInAt(LocalDateTime.now());
        rec.setStatus("ON_DUTY");
        rec.setSource(req == null || req.getSource() == null ? "MANUAL" : req.getSource());
        rec.setLocationLat(req == null ? null : req.getLat());
        rec.setLocationLng(req == null ? null : req.getLng());
        rec.setRemark(req == null ? null : req.getRemark());
        rec.setWorkMinutes(0);
        rec.setBreakMinutes(0);
        if (reSignIn) {
            // New duty period: discard the closed period's end time. NB:
            // updateById skips null fields (MyBatis-Plus default strategy), so a
            // plain setClockOutAt(null) would silently keep the old value and
            // clock-out would idempotently early-return forever (SB-N2) — the
            // column must be cleared with an explicit UPDATE. breakStartAt is
            // cleared too: any open break belonged to the closed period (SA-P2-003).
            rec.setClockOutAt(null);
            rec.setBreakStartAt(null);
            recordMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AttendanceRecord>()
                    .eq(AttendanceRecord::getId, rec.getId())
                    .set(AttendanceRecord::getClockOutAt, null)
                    .set(AttendanceRecord::getBreakStartAt, null));
        }
        applyAuditFields(rec, installerId);

        if (existing == null) {
            recordMapper.insert(rec);
        } else {
            recordMapper.updateById(rec);
        }

        // Sync installer_profile.on_duty
        InstallerProfile p = installerProfileService.getByUserId(installerId);
        if (p != null) {
            p.setOnDuty(1);
            p.setLastLocationTime(LocalDateTime.now());
            installerProfileService.updateById(p);
        }
        if (Boolean.TRUE.equals(props.getUseActiveZset())) {
            redisUtils.zAdd(ACTIVE_ZSET, String.valueOf(installerId), System.currentTimeMillis());
        }
        // SA-P1-005: clock-in means on-duty, hence dispatchable — mirror into the
        // installers:active ZSET that dispatch candidate loading reads.
        redisUtils.zAdd(ONLINE_KEY, String.valueOf(installerId), System.currentTimeMillis());

        publisher.publishEvent(new BbpmsEvents.InstallerClockInEvent(
                installerId, rec.getClockInAt(), rec.getLocationLat(), rec.getLocationLng()));
        log.info("clockIn: installer {} at {}", installerId, rec.getClockInAt());
        return enrich(rec, installerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(value = "签出", module = "考勤")
    public AttendanceVO clockOut(Long installerId, ClockOutReq req) {
        if (installerId == null) {
            throw new BizException(com.bbpms.common.enums.ResultCode.BAD_REQUEST, "installerId 必填");
        }
        AttendanceRecord rec = mustFindToday(installerId);
        if (rec.getClockOutAt() != null) {
            // Already clocked out — return as-is.
            return enrich(rec, installerId);
        }
        if (rec.getClockInAt() == null) {
            throw new BizException(com.bbpms.common.enums.ResultCode.BAD_REQUEST, "请先签到");
        }
        LocalDateTime now = LocalDateTime.now();
        // If still on break, settle the open break first so workMinutes is exact
        // (SA-P2-003): an ON_BREAK sign-out used to lose the open break entirely.
        if ("ON_BREAK".equals(rec.getStatus()) && rec.getBreakStartAt() != null) {
            settleBreak(rec, now);
        }
        rec.setClockOutAt(now);
        rec.setStatus("OFF_DUTY");
        int total = (int) Duration.between(rec.getClockInAt(), now).toMinutes();
        rec.setWorkMinutes(Math.max(0, total - (rec.getBreakMinutes() == null ? 0 : rec.getBreakMinutes())));
        rec.setUpdateBy(installerId);
        recordMapper.updateById(rec);

        InstallerProfile p = installerProfileService.getByUserId(installerId);
        if (p != null) {
            p.setOnDuty(0);
            installerProfileService.updateById(p);
        }
        if (Boolean.TRUE.equals(props.getUseActiveZset())) {
            redisUtils.zRem(ACTIVE_ZSET, String.valueOf(installerId));
        }
        // SA-P1-005: clock-out means off duty — not dispatchable anymore.
        redisUtils.zRem(ONLINE_KEY, String.valueOf(installerId));

        publisher.publishEvent(new BbpmsEvents.InstallerClockOutEvent(
                installerId, now, rec.getWorkMinutes()));
        log.info("clockOut: installer {} at {} ({} min)", installerId, now, rec.getWorkMinutes());
        return enrich(rec, installerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttendanceVO breakStart(Long installerId, BreakReq req) {
        AttendanceRecord rec = mustFindToday(installerId);
        if (!"ON_DUTY".equals(rec.getStatus())) {
            throw new BizException(com.bbpms.common.enums.ResultCode.BAD_REQUEST,
                    "当前状态 " + rec.getStatus() + " 不允许开始休息");
        }
        rec.setBreakStartAt(LocalDateTime.now());
        rec.setStatus("ON_BREAK");
        rec.setUpdateBy(installerId);
        recordMapper.updateById(rec);
        publisher.publishEvent(new BbpmsEvents.InstallerBreakEvent(installerId, "START", LocalDateTime.now()));
        return enrich(rec, installerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttendanceVO breakEnd(Long installerId, BreakReq req) {
        AttendanceRecord rec = mustFindToday(installerId);
        if (!"ON_BREAK".equals(rec.getStatus())) {
            throw new BizException(com.bbpms.common.enums.ResultCode.BAD_REQUEST,
                    "当前状态 " + rec.getStatus() + " 不允许结束休息");
        }
        // SA-P2-003: settle the real elapsed break duration instead of a fixed
        // 30 minutes. If break_start_at is missing (legacy row), fall back to 30.
        LocalDateTime now = LocalDateTime.now();
        settleBreak(rec, now);
        rec.setStatus("ON_DUTY");
        rec.setUpdateBy(installerId);
        recordMapper.updateById(rec);
        publisher.publishEvent(new BbpmsEvents.InstallerBreakEvent(installerId, "END", now));
        return enrich(rec, installerId);
    }

    @Override
    public AttendanceVO today(Long installerId) {
        AttendanceRecord rec = recordMapper.findByInstallerAndDate(installerId, LocalDate.now());
        return enrich(rec, installerId);
    }

    @Override
    public List<AttendanceVO> daily(Long installerId, int limit) {
        if (limit <= 0) limit = 30;
        return recordMapper.findRecent(installerId, limit).stream()
                .map(r -> enrich(r, installerId))
                .collect(Collectors.toList());
    }

    @Override
    public AttendanceSummaryVO monthly(Long installerId, String yearMonth) {
        if (yearMonth == null || yearMonth.isBlank()) {
            yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        AttendanceSummary s = summaryMapper.findByInstallerAndMonth(installerId, yearMonth);
        if (s == null) {
            // SA-P2-001: no rollup exists yet — aggregate on demand from the
            // daily records and persist it (lazy writer).
            s = rollup(installerId, yearMonth);
            if (s != null) {
                applyAuditFields(s, installerId);
                summaryMapper.insert(s);
            }
        }
        return AttendanceSummaryVO.from(s);
    }

    /**
     * Aggregate one installer's daily records for the month (SA-P2-001).
     * Absent days = elapsed weekdays (before today) minus signed-in days.
     */
    private AttendanceSummary rollup(Long installerId, String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();
        int lateThreshold = props.getLateThresholdMinutes() == null ? 15 : props.getLateThresholdMinutes();
        String offDuty = props.getOffDutyTime() == null ? "17:00" : props.getOffDutyTime();

        AttendanceMonthlyStat stat = recordMapper.aggregateMonth(
                installerId, monthStart, monthEnd, lateThreshold, offDuty);
        if (stat == null) return null;

        // Weekdays elapsed so far this month (today excluded — the day is not
        // over, so a missing clock-in is not yet an absence).
        int elapsedWeekdays = 0;
        LocalDate today = LocalDate.now();
        LocalDate d = monthStart;
        while (!d.isAfter(today) && !d.isAfter(monthEnd)) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY && !d.equals(today)) {
                elapsedWeekdays++;
            }
            d = d.plusDays(1);
        }
        int workDays = stat.getWorkDays() == null ? 0 : stat.getWorkDays();

        AttendanceSummary s = new AttendanceSummary();
        s.setInstallerId(installerId);
        s.setYearMonth(yearMonth);
        s.setTotalWorkMinutes(stat.getTotalWorkMinutes() == null ? 0 : stat.getTotalWorkMinutes());
        s.setWorkDays(workDays);
        s.setLateCount(stat.getLateCount() == null ? 0 : stat.getLateCount());
        s.setEarlyLeaveCount(stat.getEarlyLeaveCount() == null ? 0 : stat.getEarlyLeaveCount());
        s.setAbsentCount(Math.max(0, elapsedWeekdays - workDays));
        s.setLastCalculatedAt(LocalDateTime.now());
        return s;
    }

    @Override
    public List<AttendanceVO> onDutyNow() {
        LambdaQueryWrapper<AttendanceRecord> w = new LambdaQueryWrapper<>();
        w.in(AttendanceRecord::getStatus, "ON_DUTY", "ON_BREAK")
         .eq(AttendanceRecord::getWorkDate, LocalDate.now());
        return recordMapper.selectList(w).stream()
                .map(r -> enrich(r, r.getInstallerId()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isOnDuty(Long installerId, LocalDate on) {
        if (on == null) on = LocalDate.now();
        AttendanceRecord r = recordMapper.findByInstallerAndDate(installerId, on);
        if (r == null) return false;
        return "ON_DUTY".equals(r.getStatus()) || "ON_BREAK".equals(r.getStatus());
    }

    /* ---------- internals ---------- */

    /**
     * Accumulate the elapsed break into breakMinutes and clear breakStartAt.
     * The record's other fields are untouched; caller persists (SA-P2-003).
     */
    private void settleBreak(AttendanceRecord rec, LocalDateTime now) {
        int prev = rec.getBreakMinutes() == null ? 0 : rec.getBreakMinutes();
        if (rec.getBreakStartAt() == null) {
            rec.setBreakMinutes(prev + 30); // legacy row without break_start_at
        } else {
            long elapsed;
            try {
                elapsed = Duration.between(rec.getBreakStartAt(), now).toMinutes();
            } catch (Exception ex) {
                elapsed = 0;
            }
            rec.setBreakMinutes(prev + Math.max(1, (int) elapsed));
        }
        rec.setBreakStartAt(null);
    }

    private AttendanceRecord mustFindToday(Long installerId) {
        AttendanceRecord rec = recordMapper.findByInstallerAndDate(installerId, LocalDate.now());
        if (rec == null) {
            throw new BizException(com.bbpms.common.enums.ResultCode.BAD_REQUEST,
                    "今日未签到，请先签到");
        }
        return rec;
    }

    private AttendanceVO enrich(AttendanceRecord r, Long installerId) {
        if (r == null) return null;
        AttendanceVO vo = AttendanceVO.from(r);
        if (vo == null) return null;
        try {
            com.bbpms.user.entity.SysUser u = sysUserMapper.selectById(installerId);
            if (u != null) vo.setInstallerName(u.getRealName() == null ? u.getUsername() : u.getRealName());
        } catch (Exception ignored) { /* best-effort */ }
        if (r.getClockInAt() != null) {
            try {
                int hm = Integer.parseInt(r.getClockInAt().toLocalTime().format(HM).replace(":", ""));
                int threshold = 9 * 100 + (props.getLateThresholdMinutes() == null ? 15 : props.getLateThresholdMinutes());
                vo.setIsLate(hm > threshold);
            } catch (Exception ignored) { /* best-effort */ }
        }
        return vo;
    }

    private void applyAuditFields(BaseDO d, Long userId) {
        Long uid = userId != null ? userId : SecurityUtils.getCurrentUserId();
        d.setCreateBy(uid);
        d.setUpdateBy(uid);
    }

    /** List used by admin endpoint; needed to satisfy older signature. */
    @SuppressWarnings("unused")
    private List<AttendanceVO> safeEmpty() {
        return Collections.emptyList();
    }

    /** Compile-safe ignore of BigDecimal. */
    @SuppressWarnings("unused")
    private BigDecimal zero() {
        return BigDecimal.ZERO;
    }
}
