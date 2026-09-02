import request from '@/utils/request'

export interface DashboardKpi {
  todayOrders: number
  todayAuditPending: number
  runningWorkorders: number
  completedToday: number
}

export interface TrendPoint {
  date: string
  count: number
}

export interface StatusSlice {
  status: string
  count: number
}

export interface EfficiencyBucket {
  bucket: string
  count: number
}

export function getDashboardKpi() {
  return request<DashboardKpi>({
    url: '/dashboard/kpi',
    method: 'GET'
  })
}

export function getOrderTrend(days = 7) {
  return request<TrendPoint[]>({
    url: '/dashboard/order-trend',
    method: 'GET',
    params: { days }
  })
}

export function getOrderStatusDist() {
  return request<StatusSlice[]>({
    url: '/dashboard/order-status-dist',
    method: 'GET'
  })
}

export function getDispatchEfficiency() {
  return request<EfficiencyBucket[]>({
    url: '/dashboard/dispatch-efficiency',
    method: 'GET'
  })
}