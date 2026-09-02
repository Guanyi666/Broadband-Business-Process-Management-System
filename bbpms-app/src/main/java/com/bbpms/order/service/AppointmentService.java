package com.bbpms.order.service;

import com.bbpms.order.entity.Appointment;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Appointment service. Persists scheduled install slots on the
 * {@code appointment} table — never on the order row itself to avoid
 * polluting the audit history with date slips.
 */
public interface AppointmentService {

    /**
     * Create a new appointment for an order. Throws if orderId or time is null.
     */
    Appointment create(Long orderId, LocalDateTime time, String phone, String remark);

    /**
     * Find all appointments for an order, newest first.
     */
    List<Appointment> findByOrderId(Long orderId);

    /**
     * Update the most-recent appointment's time / contact phone / remark.
     * If no appointment exists, a new one is inserted.
     */
    Appointment update(Long orderId, LocalDateTime time, String contactPhone, String remark);
}