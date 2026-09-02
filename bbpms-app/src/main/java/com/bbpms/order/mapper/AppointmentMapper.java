package com.bbpms.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bbpms.order.entity.Appointment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AppointmentMapper extends BaseMapper<Appointment> {

    List<Appointment> selectByOrderId(@Param("orderId") Long orderId);
}
