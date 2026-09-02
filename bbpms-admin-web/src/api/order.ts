import request from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/common'
import type { OrderItem, OrderTimelineItem, OrderStatus } from '@/types/order'

export interface OrderCreatePayload {
  customerId?: number | string
  customerName?: string
  customerPhone?: string
  customerIdCard?: string
  address: string
  lng?: number
  lat?: number
  packageId: number | string
  appointmentAt: string
  remark?: string
  source?: string
}

export interface AuditPayload {
  pass: boolean
  remark?: string
}

export function pageOrders(params: PageQuery & { status?: OrderStatus; csId?: number | string; dateRange?: string[] }) {
  return request<PageResult<OrderItem>>({
    url: '/orders/page',
    method: 'GET',
    params
  })
}

export function createOrder(data: OrderCreatePayload) {
  return request<OrderItem>({ url: '/orders', method: 'POST', data })
}

export function getOrderDetail(id: number | string) {
  return request<OrderItem & { timeline: OrderTimelineItem[]; customer?: any; workorder?: any }>({
    url: `/orders/${id}`,
    method: 'GET'
  })
}

export function auditOrder(id: number | string, data: AuditPayload) {
  return request<void>({ url: `/orders/${id}/audit`, method: 'POST', data })
}

export function cancelOrder(id: number | string, remark?: string) {
  return request<void>({ url: `/orders/${id}/cancel`, method: 'POST', data: { remark } })
}

export function orderTimeline(id: number | string) {
  return request<OrderTimelineItem[]>({
    url: `/orders/${id}/timeline`,
    method: 'GET'
  })
}