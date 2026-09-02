import request from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/common'
import type { WorkorderItem } from '@/types/order'

/** Backend pagination envelope (com.bbpms.common.result.PageResp). */
interface PageResp<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

export async function pageWorkorders(
  params: PageQuery & {
    status?: WorkorderItem['status']
    installerId?: number | string
    startTime?: string
    endTime?: string
  }
): Promise<PageResult<WorkorderItem>> {
  const res = await request<PageResp<WorkorderItem>>({
    url: '/work-orders/page',
    method: 'GET',
    params
  })
  // adapter: backend PageResp → admin PageResult ({list,total,...})
  return { list: res?.records ?? [], total: res?.total ?? 0, pageNum: res?.pageNum ?? 1, pageSize: res?.pageSize ?? 20 }
}

export function getWorkorderDetail(id: number | string) {
  return request<WorkorderItem>({
    url: `/work-orders/${id}`,
    method: 'GET'
  })
}

export function acceptWorkorder(id: number | string) {
  return request<void>({ url: `/work-orders/${id}/accept`, method: 'POST' })
}

export function startWorkorder(id: number | string) {
  return request<void>({ url: `/work-orders/${id}/start`, method: 'POST' })
}

// Backend /work-orders/{id}/complete takes a query param installRecordId and
// returns R<Void>; the installer-facing completion flow lives under /install.
export function completeWorkorder(id: number | string, installRecordId?: number | string) {
  return request<void>({
    url: `/work-orders/${id}/complete`,
    method: 'POST',
    params: installRecordId != null ? { installRecordId } : undefined
  })
}

// Backend transfer = return the order to the pool (WorkOrderTransferReq{workOrderId, reason});
// to dispatch to a specific installer use reassignWorkOrder below.
export function transferWorkorder(id: number | string, reason: string) {
  return request<void>({
    url: `/work-orders/${id}/transfer`,
    method: 'POST',
    data: { workOrderId: Number(id), reason }
  })
}

// Backend /work-orders/{id}/cancel takes reason as a query param.
export function cancelWorkorder(id: number | string, reason?: string) {
  return request<void>({
    url: `/work-orders/${id}/cancel`,
    method: 'POST',
    params: reason ? { reason } : undefined
  })
}

/* ---------- Phase 5 SLA / lifecycle additions ---------- */

export interface WorkOrderVO {
  id: number
  workNo: string
  orderId: number
  installerId?: number
  installerName?: string
  status: string
  statusDesc?: string
  dispatchTime?: string
  acceptTime?: string
  startTime?: string
  finishTime?: string
  installAddress?: string
  expectedFinishTime?: string
  lastActiveAt?: string
  stallReason?: string
  cancelType?: string
  priority?: number
}

export function fetchExpiring(minutes = 30) {
  return request<WorkOrderVO[]>({ url: `/work-orders/expiring`, method: 'GET', params: { minutes } })
}

export function reassignWorkOrder(id: number, payload: { newInstallerId: number; reason: string }) {
  return request<WorkOrderVO>({ url: `/work-orders/${id}/reassign`, method: 'POST', data: payload })
}

export function forceCloseWorkOrder(id: number, payload: { reason: string; cancelType: string }) {
  return request<void>({ url: `/work-orders/${id}/force-close`, method: 'POST', data: payload })
}

export function resumeWorkOrder(id: number) {
  return request<WorkOrderVO>({ url: `/work-orders/${id}/resume`, method: 'POST' })
}

export function pageWorkOrdersSla(params: { pageNum: number; pageSize: number; status?: string }) {
  return request<PageResp<WorkOrderVO>>({
    url: `/work-orders/page`,
    method: 'GET',
    params
  })
}
