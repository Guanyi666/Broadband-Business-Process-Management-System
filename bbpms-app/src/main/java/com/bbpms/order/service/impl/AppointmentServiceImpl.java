package com.bbpms.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bbpms.common.enums.ResultCode;
import com.bbpms.common.exception.BizException;
import com.bbpms.common.util.SecurityUtils;
import com.bbpms.order.entity.Appointment;
import com.bbpms.order.mapper.AppointmentMapper;
import com.bbpms.order.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentMapper appointmentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Appointment create(Long orderId, LocalDateTime time, String phone, String remark) {
        if (orderId == null || time == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "orderId 和 appointmentTime 必填");
        }
        Appointment a = new Appointment();
        a.setOrderId(orderId);
        a.setAppointmentTime(time);
        a.setContactPhone(phone);
        a.setRemark(remark);
        a.setConfirmed(0);
        a.setStatus("PENDING");
        a.setRescheduleCount(0);
        a.setCreateBy(SecurityUtils.getCurrentUserId());
        a.setUpdateBy(SecurityUtils.getCurrentUserId());
        appointmentMapper.insert(a);
        return a;
    }

    @Override
    public List<Appointment> findByOrderId(Long orderId) {
        return appointmentMapper.selectByOrderId(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Appointment update(Long orderId, LocalDateTime time, String contactPhone, String remark) {
        if (orderId == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "orderId 必填");
        }
        Long uid = SecurityUtils.getCurrentUserId();
        Appointment latest = appointmentMapper.selectOne(
                new LambdaQueryWrapper<Appointment>().eq(Appointment::getOrderId, orderId)
                        .orderByDesc(Appointment::getAppointmentTime)
                        .last("LIMIT 1"));
        if (latest == null) {
            Appointment a = new Appointment();
            a.setOrderId(orderId);
            a.setAppointmentTime(time);
            a.setContactPhone(contactPhone);
            a.setRemark(remark);
            a.setConfirmed(0);
            a.setStatus("PENDING");
            a.setRescheduleCount(0);
            a.setCreateBy(uid);
            a.setUpdateBy(uid);
            appointmentMapper.insert(a);
            return a;
        }
        if (time != null)         latest.setAppointmentTime(time);
        if (contactPhone != null) latest.setContactPhone(contactPhone);
        if (remark != null)       latest.setRemark(remark);
        latest.setUpdateBy(uid);
        appointmentMapper.updateById(latest);
        return latest;
    }
}
