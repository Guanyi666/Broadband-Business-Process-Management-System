package com.bbpms.install.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bbpms.common.annotation.OperationLog;
import com.bbpms.common.enums.ResultCode;
import com.bbpms.common.event.BbpmsEvents;
import com.bbpms.common.exception.BizException;
import com.bbpms.common.result.PageResp;
import com.bbpms.common.util.JsonUtils;
import com.bbpms.common.util.RedisUtils;
import com.bbpms.install.bss.BssClient;
import com.bbpms.install.config.InstallProperties;
import com.bbpms.install.dto.BssActivateReq;
import com.bbpms.install.dto.InstallArriveReq;
import com.bbpms.install.dto.InstallCompleteReq;
import com.bbpms.install.dto.InstallInfoReq;
import com.bbpms.install.dto.InstallPageReq;
import com.bbpms.install.dto.InstallPhotoReq;
import com.bbpms.install.dto.InstallSignatureReq;
import com.bbpms.install.entity.InstallRecord;
import com.bbpms.install.mapper.InstallRecordMapper;
import com.bbpms.install.service.InstallService;
import com.bbpms.install.vo.InstallProgressVO;
import com.bbpms.install.vo.InstallRecordVO;
import com.bbpms.order.service.OrderService;
import com.bbpms.workorder.entity.WorkOrder;
import com.bbpms.workorder.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Install / provisioning orchestrator.
 *
 * <p>{@link #complete} is the saga replacement: writes the install record,
 * transitions the work order to COMPLETED, activates the BSS, then closes
 * the order. On BSS failure the work order is reverted to IN_PROGRESS and the
 * exception is rethrown so the caller can react.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstallServiceImpl implements InstallService {

    private final WorkOrderService workOrderService;
    private final OrderService orderService;
    private final InstallRecordMapper recordMapper;
    private final BssClient bssClient;
    private final ApplicationEventPublisher publisher;
    private final RedisUtils redisUtils;
    private final InstallProperties props;

    @Override
    @OperationLog(module = "install", value = "到达现场")
    @Transactional(rollbackFor = Exception.class)
    public void arrive(Long workOrderId, InstallArriveReq req, Long installerId) {
        InstallRecord rec = mustFind(workOrderId);
        if (installerId != null) rec.setInstallerId(installerId);
        if (req != null) {
            rec.setStartLat(req.getLat());
            rec.setStartLng(req.getLng());
            rec.setRemark(req.getAddress());
        }
        rec.setStatus("IN_PROGRESS");
        recordMapper.updateById(rec);
    }

    @Override
    @OperationLog(module = "install", value = "保存安装信息")
    @Transactional(rollbackFor = Exception.class)
    public void saveInfo(Long workOrderId, InstallInfoReq req) {
        if (req == null) return;
        InstallRecord rec = mustFind(workOrderId);
        rec.setOnuMac(req.getOnuMac());
        rec.setOnuSn(req.getOnuSn());
        rec.setOltPort(req.getOltPort());
        rec.setSignalDb(req.getSignal());
        recordMapper.updateById(rec);
    }

    @Override
    @OperationLog(module = "install", value = "添加照片")
    @Transactional(rollbackFor = Exception.class)
    public void addPhoto(Long workOrderId, InstallPhotoReq req) {
        if (req == null || !StringUtils.hasText(req.getObjectKey()) && !StringUtils.hasText(req.getUrl())) {
            throw new BizException(ResultCode.BAD_REQUEST, "photo objectKey/url 不能为空");
        }
        InstallRecord rec = mustFind(workOrderId);
        List<String> photos = parsePhotos(rec.getPhotos());
        String key = StringUtils.hasText(req.getObjectKey()) ? req.getObjectKey() : req.getUrl();
        photos.add(key);
        rec.setPhotos(JsonUtils.toJson(photos));
        recordMapper.updateById(rec);
    }

    @Override
    @OperationLog(module = "install", value = "添加签名")
    @Transactional(rollbackFor = Exception.class)
    public void addSignature(Long workOrderId, InstallSignatureReq req) {
        if (req == null) throw new BizException(ResultCode.BAD_REQUEST, "signature 不能为空");
        InstallRecord rec = mustFind(workOrderId);
        rec.setSignatureUrl(StringUtils.hasText(req.getObjectKey()) ? req.getObjectKey() : req.getDataUrl());
        rec.setCustomerSignatureName(req.getCustomerName());
        recordMapper.updateById(rec);
    }

    @Override
    @OperationLog(module = "install", value = "完成工单")
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long workOrderId, InstallCompleteReq req, Long installerId) {
        if (req == null) throw new BizException(ResultCode.BAD_REQUEST, "请求不能为空");

        InstallRecord rec = mustFind(workOrderId);

        // 1. GPS distance soft-check (warn but allow)
        double gpsMeters = haversineMeters(rec.getCompleteLat(), rec.getCompleteLng(), req.getLat(), req.getLng());
        InstallProperties.Gps gps = props.getGps();
        if (gps != null && gps.getThresholdMeters() != null && gpsMeters > gps.getThresholdMeters()) {
            log.warn("install gps distance {}m exceeds threshold {}m for workOrderId={}",
                    Math.round(gpsMeters), gps.getThresholdMeters(), workOrderId);
        }

        // 2. Photos count check
        List<String> photos = parsePhotos(rec.getPhotos());
        InstallProperties.Photos pconf = props.getPhotos();
        int minCount = pconf == null || pconf.getMinCount() == null ? 3 : pconf.getMinCount();
        if (photos.size() < minCount) {
            throw new BizException(ResultCode.BAD_REQUEST,
                    "至少需要 " + minCount + " 张现场照片");
        }

        // 3. Signal soft-check (warn but allow)
        InstallProperties.Signal sconf = props.getSignal();
        BigDecimal signal = req.getInfo() != null ? req.getInfo().getSignal() : rec.getSignalDb();
        if (signal != null && sconf != null && sconf.getWarnDbm() != null
                && signal.intValue() <= sconf.getWarnDbm()) {
            log.warn("install signal {} dBm below warn threshold {} dBm for workOrderId={}",
                    signal, sconf.getWarnDbm(), workOrderId);
        }

        // 4. Persist completion fields
        if (req.getInfo() != null) {
            rec.setOnuMac(req.getInfo().getOnuMac());
            rec.setOnuSn(req.getInfo().getOnuSn());
            rec.setOltPort(req.getInfo().getOltPort());
            rec.setSignalDb(req.getInfo().getSignal());
        }
        if (req.getSignature() != null) {
            rec.setCustomerSignatureName(req.getSignature().getCustomerName());
            rec.setSignatureUrl(StringUtils.hasText(req.getSignature().getObjectKey())
                    ? req.getSignature().getObjectKey() : req.getSignature().getDataUrl());
        }
        rec.setCompleteLat(req.getLat());
        rec.setCompleteLng(req.getLng());
        rec.setRemark(req.getRemark());
        rec.setSubmitTime(LocalDateTime.now());
        rec.setStatus("COMPLETED");
        recordMapper.updateById(rec);

        // 5. Transition the work order to COMPLETED
        workOrderService.complete(workOrderId, installerId, rec.getId());

        // 6. BSS activation (saga-replacement step)
        try {
            BssActivateReq bssReq = new BssActivateReq();
            bssReq.setOrderId(req.getOrderId());
            // Best-effort: pull a friendly name from the work-order detail
            try {
                bssReq.setCustomerName(
                        workOrderService.getDetail(workOrderId).getCustomerName());
                bssReq.setAddress(
                        workOrderService.getDetail(workOrderId).getInstallAddress());
            } catch (Exception ignored) {
                // tolerate missing enrichment
            }
            bssClient.activate(bssReq);
        } catch (Exception ex) {
            log.error("BSS activation failed for workOrderId={}, reverting to IN_PROGRESS: {}",
                    workOrderId, ex.getMessage());
            rec.setStatus("IN_PROGRESS");
            recordMapper.updateById(rec);
            try {
                workOrderService.revertToInstalling(workOrderId);
            } catch (Exception e) {
                log.error("revertToInstalling failed for workOrderId={}: {}", workOrderId, e.getMessage());
            }
            throw ex;
        }

        // 7. Close the order
        try {
            orderService.markFinished(req.getOrderId(), installerId);
        } catch (Exception e) {
            log.error("markFinished failed for orderId={}: {}", req.getOrderId(), e.getMessage());
        }

        // 8. InstallCompleted event for downstream listeners
        publisher.publishEvent(new BbpmsEvents.InstallCompletedEvent(
                workOrderId, req.getOrderId(), installerId, rec.getOnuMac()));

        // 9. Notify customer
        Map<String, Object> params = new HashMap<>();
        params.put("workOrderId", workOrderId);
        params.put("onuMac", rec.getOnuMac());
        publisher.publishEvent(new BbpmsEvents.NotifyEvent(
                "SMS", null, null, null, "INSTALL_COMPLETED_SMS", params));
    }

    @Override
    public InstallRecordVO getByWorkOrderId(Long workOrderId) {
        InstallRecord rec = recordMapper.selectByWorkOrderId(workOrderId);
        return rec == null ? null : toVO(rec);
    }

    @Override
    public PageResp<InstallRecordVO> page(InstallPageReq req) {
        IPage<InstallRecord> page = new Page<>(
                req.getPageNum() == null ? 1 : req.getPageNum(),
                req.getPageSize() == null ? 20 : req.getPageSize());
        IPage<InstallRecord> result = recordMapper.selectPageWithScope(page, req);
        List<InstallRecordVO> vos = new ArrayList<>();
        for (InstallRecord r : result.getRecords()) vos.add(toVO(r));
        IPage<InstallRecordVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(vos);
        return PageResp.of(voPage);
    }

    @Override
    public List<InstallRecordVO> getByInstaller(Long installerId) {
        List<InstallRecord> rows = recordMapper.selectByInstaller(installerId);
        List<InstallRecordVO> vos = new ArrayList<>();
        for (InstallRecord r : rows) vos.add(toVO(r));
        return vos;
    }

    @Override
    public InstallProgressVO getProgress(Long workOrderId) {
        InstallRecord rec = recordMapper.selectByWorkOrderId(workOrderId);
        InstallProgressVO vo = new InstallProgressVO();
        vo.setWorkOrderId(workOrderId);
        vo.setLastUpdated(rec == null ? null : rec.getUpdateTime());
        vo.setTotalSteps(5);
        vo.setStatus(rec == null ? "UNKNOWN" : rec.getStatus());
        // Derive current step from flags
        int step = 1;
        if (rec != null) {
            if (rec.getStartLat() != null && rec.getStartLng() != null) step = 2;
            if (StringUtils.hasText(rec.getOnuMac()) || StringUtils.hasText(rec.getOltPort())) step = 3;
            if (!parsePhotos(rec.getPhotos()).isEmpty()) step = 4;
            if (StringUtils.hasText(rec.getSignatureUrl())) step = 5;
            if ("COMPLETED".equalsIgnoreCase(rec.getStatus())) step = 5;
        }
        vo.setCurrentStep(stepName(step));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initRecord(Long workOrderId, Long installerId) {
        if (workOrderId == null) return;
        InstallRecord existing = recordMapper.selectByWorkOrderId(workOrderId);
        if (existing != null) return; // idempotent
        InstallRecord rec = new InstallRecord();
        rec.setWorkOrderId(workOrderId);
        rec.setInstallerId(installerId);
        rec.setStatus("PENDING");
        rec.setPhotos("[]");
        recordMapper.insert(rec);
    }

    /* ===================== helpers ===================== */

    private InstallRecord mustFind(Long workOrderId) {
        if (workOrderId == null) throw new BizException(ResultCode.BAD_REQUEST, "workOrderId 不能为空");
        InstallRecord rec = recordMapper.selectByWorkOrderId(workOrderId);
        if (rec == null) {
            // Auto-init so the installer flow is seamless
            rec = new InstallRecord();
            rec.setWorkOrderId(workOrderId);
            rec.setStatus("PENDING");
            rec.setPhotos("[]");
            recordMapper.insert(rec);
        }
        return rec;
    }

    private InstallRecordVO toVO(InstallRecord r) {
        InstallRecordVO vo = new InstallRecordVO();
        BeanUtils.copyProperties(r, vo);
        List<String> photos = parsePhotos(r.getPhotos());
        vo.setPhotos(photos);
        vo.setPhotoCount(photos.size());
        return vo;
    }

    private static List<String> parsePhotos(String json) {
        if (!StringUtils.hasText(json)) return new ArrayList<>();
        try {
            return new ArrayList<>(JsonUtils.parseList(json, String.class));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static String stepName(int n) {
        return switch (n) {
            case 1 -> "arrive";
            case 2 -> "info";
            case 3 -> "photos";
            case 4 -> "signature";
            case 5 -> "complete";
            default -> "unknown";
        };
    }

    /**
     * Haversine in METERS between two (lat, lng) pairs. Null inputs are
     * treated as zero distance (caller decides how to interpret).
     */
    private static double haversineMeters(BigDecimal lat1, BigDecimal lng1,
                                          BigDecimal lat2, BigDecimal lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) return 0.0;
        double R = 6371008.8; // Earth radius in metres
        double phi1 = Math.toRadians(lat1.doubleValue());
        double phi2 = Math.toRadians(lat2.doubleValue());
        double dPhi = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLam = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());
        double a = Math.sin(dPhi / 2) * Math.sin(dPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLam / 2) * Math.sin(dLam / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}