package com.bbpms.track.service;

import com.bbpms.track.vo.TrackResultVO;

/**
 * 订单 / 工单「订单与履约双轨时间线」轨迹合成服务。
 *
 * <p>设计要点（见 {@code docs/TIMELINE_ANALYSIS.md}）：
 * <ul>
 *   <li>骨架节点时间全部取自订单表 / 工单表的真实时间字段，为空即 null + PENDING，不伪造。</li>
 *   <li>操作人优先取主表人员字段（customer_id / auditor_id / cs_id / dispatcher_id / installer_id），
 *       日志表 operator_id 全为 NULL，仅作兜底。</li>
 *   <li>姓名解析优先级：{@code nickname → real_name → username}。</li>
 *   <li>自动 / 人工判定：operator_role=SYSTEM 或 dispatch_record.strategy=AUTO → 自动。</li>
 *   <li>1 订单多工单：取「主工单」= 非 CANCELLED/AUTO_CANCELLED 的最新一条用于主干；
 *       其余工单的流转记录作为 events 展示。</li>
 * </ul>
 */
public interface TrackService {

    /** 订单履约轨迹（8 节点骨架 + 事件 + 汇总）。{@code orderId} 为 broadband_order.id。 */
    TrackResultVO getOrderTrack(Long orderId);

    /** 工单履约轨迹（5 节点骨架 + 事件 + 汇总）。{@code workOrderId} 为 work_order.id。 */
    TrackResultVO getWorkOrderTrack(Long workOrderId);
}
