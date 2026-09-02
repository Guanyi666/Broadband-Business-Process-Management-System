package com.bbpms.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bbpms.order.entity.OrderAuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderAuditLogMapper extends BaseMapper<OrderAuditLog> {

    List<OrderAuditLog> selectByOrderId(@Param("orderId") Long orderId);

    int batchInsert(@Param("rows") List<OrderAuditLog> rows);
}
