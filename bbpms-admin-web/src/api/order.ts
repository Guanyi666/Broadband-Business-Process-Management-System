import request from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/common'
import type { OrderItem, OrderTimelineItem, OrderStatus } from '@/types/order'

export interface OrderCreatePayload {
  customerId: number | string
  packageCode: string
  packageName?: string
  installAddress: string
  expectedInstallDate?: string
  appointmentTime?: string
  contactPhone?: string
  remark?: string
}

export interface AuditPayload {
  pass: boolean
  remark?: string
}

interface PageResp<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
}

interface BackendOrder {
  id: number | string
  orderNo: string
  customerId: number | string
  customerName?: string
  packageCode?: string
  packageName?: string
  installAddress?: string
  expectedInstallDate?: string
  status: OrderStatus
  csId?: number | string
  auditorId?: number | string
  auditTime?: string
  auditRemark?: string
  createTime: string
  updateTime?: string
}

function toOrderItem(v: BackendOrder): OrderItem {
  return {
    ...v,
    address: v.installAddress,
    appointmentAt: v.expectedInstallDate,
    auditBy: v.auditorId,
    auditAt: v.auditTime,
    remark: v.auditRemark,
    createdAt: v.createTime,
    updatedAt: v.updateTime
  }
}

export async function pageOrders(params: PageQuery & { status?: OrderStatus; csId?: number | string; startTime?: string; endTime?: string }) {
  const res = await request<PageResp<BackendOrder>>({
    url: '/orders/page',
    method: 'GET',
    params
  })
  return {
    list: (res?.records ?? []).map(toOrderItem),
    total: res?.total ?? 0,
    pageNum: res?.pageNum ?? 1,
    pageSize: res?.pageSize ?? 10
  } as PageResult<OrderItem>
}

export function createOrder(data: OrderCreatePayload) {
  return request<number | string>({ url: '/orders', method: 'POST', data })
}

export async function getOrderDetail(id: number | string) {
  const detail = await request<any>({
    url: `/orders/${id}`,
    method: 'GET'
  })
  const order = toOrderItem(detail.order)
  return {
    ...order,
    customerName: detail.customer?.name || order.customerName,
    customerPhone: detail.customer?.phone,
    customer: detail.customer ? { ...detail.customer, idCard: detail.customer.idCardNo } : undefined,
    appointmentAt: detail.appointment?.appointmentTime || order.appointmentAt,
    contactPhone: detail.appointment?.contactPhone,
    timeline: (detail.timeline || []).map((t: any, index: number) => ({
      id: `${id}-${index}`,
      orderId: id,
      action: t.eventType || t.description,
      operator: t.operatorName,
      operatorName: t.operatorName,
      remark: t.description,
      createdAt: t.eventTime
    })) as OrderTimelineItem[]
  }
}

export function auditOrder(id: number | string, data: AuditPayload) {
  return request<void>({ url: `/orders/${id}/audit`, method: 'POST', data })
}

export function cancelOrder(id: number | string, remark?: string) {
  return request<void>({ url: `/orders/${id}/cancel`, method: 'POST', data: { reason: remark || '操作员取消订单' } })
}

export function resubmitOrder(id: number | string) {
  return request<void>({ url: `/orders/${id}/resubmit`, method: 'POST' })
}

export function orderTimeline(id: number | string) {
  return request<OrderTimelineItem[]>({
    url: `/orders/${id}/timeline`,
    method: 'GET'
  })
}
