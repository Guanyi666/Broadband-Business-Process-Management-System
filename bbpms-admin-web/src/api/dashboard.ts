import request from '@/utils/request'
import type {
  DashboardDispatch,
  DashboardOverview,
  DashboardTrend
} from '@/types/dashboard'

/**
 * Dashboard 只读统计接口（设计文档 §3.4，对齐后端 DashboardController）
 *
 * 约定：
 *  - 全部 GET、只读；days 由后端归一化（默认 7，窗口 [1,90]）
 *  - 权限：dashboard:view（无权限/未认证时 request 层统一 401/403 提示）
 */

/** 总览：6 个 KPI + 订单/工单状态分布（存量全量口径） */
export function getOverview(days = 7) {
  return request<DashboardOverview>({
    url: '/dashboard/overview',
    method: 'GET',
    params: { days }
  })
}

/** 7/14/30 日趋势：订单新建 vs 工单完成（缺失日后端已补 0，三数组等长） */
export function getTrend(days = 7) {
  return request<DashboardTrend>({
    url: '/dashboard/trend',
    method: 'GET',
    params: { days }
  })
}

/** 派单时效分桶（固定 5 桶，含 count=0） */
export function getDispatchDuration(days = 7) {
  return request<DashboardDispatch>({
    url: '/dashboard/dispatch-duration',
    method: 'GET',
    params: { days }
  })
}
