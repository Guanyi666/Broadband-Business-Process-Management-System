package com.bbpms.attendance.job;

import com.bbpms.attendance.config.AttendanceProperties;
import com.bbpms.attendance.entity.AttendanceRecord;
import com.bbpms.attendance.mapper.AttendanceRecordMapper;
import com.bbpms.common.event.BbpmsEvents;
import com.bbpms.common.util.RedisUtils;
import com.bbpms.user.entity.InstallerProfile;
import com.bbpms.user.service.InstallerProfileService;
import com.bbpms.user.service.impl.InstallerProfileServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled jobs for the attendance module.
 *
 * <p>This is the first @Scheduled class in the project — the {@code @EnableScheduling}
 * annotation on {@code BbpmsApplication} is finally in use.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceScheduleJob {

    public static final String ACTIVE_ZSET = "attendance:active";

    private final AttendanceRecordMapper recordMapper;
    private final InstallerProfileService installerProfileService;
    private final RedisUtils redisUtils;
    private final AttendanceProperties props;
    private final ApplicationEventPublisher publisher;

    /**
     * Auto sign-out installers who have been on duty for more than
     * {@code bbpms.attendance.auto-checkout-hours} without a clock-out.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000L, initialDelay = 60_000L)
    @Transactional(rollbackFor = Exception.class)
    public void autoCheckoutStale() {
        Integer hours = props.getAutoCheckoutHours();
        if (hours == null || hours <= 0) hours = 8;
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        List<AttendanceRecord> stale = recordMapper.findStaleOnDuty(cutoff);
        if (stale.isEmpty()) return;
        log.info("autoCheckoutStale: {} stale records (cutoff={})", stale.size(), cutoff);
        for (AttendanceRecord r : stale) {
            try {
                recordMapper.autoClockOut(r.getId(), LocalDateTime.now());
                InstallerProfile p = installerProfileService.getByUserId(r.getInstallerId());
                if (p != null) {
                    p.setOnDuty(0);
                    installerProfileService.updateById(p);
                }
                redisUtils.zRem(ACTIVE_ZSET, String.valueOf(r.getInstallerId()));
                // SA-P1-005: auto sign-out must also drop the dispatch online key.
                redisUtils.zRem(InstallerProfileServiceImpl.ONLINE_KEY, String.valueOf(r.getInstallerId()));
                publisher.publishEvent(new BbpmsEvents.InstallerClockOutEvent(
                        r.getInstallerId(), LocalDateTime.now(),
                        r.getWorkMinutes() == null ? 0 : r.getWorkMinutes()));
            } catch (Exception ex) {
                log.error("autoClockOut failed for recordId={}: {}", r.getId(), ex.getMessage());
            }
        }
    }

    /**
     * Purge attendance records older than the retention window.
     * Runs daily at 03:00.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldRecords() {
        Integer days = props.getHistoryRetentionDays();
        if (days == null || days <= 0) days = 90;
        LocalDate cutoff = LocalDate.now().minusDays(days);
        int deleted = recordMapper.purgeBefore(cutoff);
        log.info("cleanupOldRecords: deleted {} records before {}", deleted, cutoff);
    }
}
