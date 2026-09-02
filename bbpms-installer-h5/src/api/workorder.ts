import { get, post } from './http'
import type { WorkOrder, WorkOrderListParams, WorkOrderPage } from '@/types/workorder'

/** Backend pagination envelope (com.bbpms.common.result.PageResp). */
interface PageResp<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

/** GET /api/work-orders/my — adapter: backend PageResp → H5 page shape. */
export async function fetchMyWorkOrders(params: WorkOrderListParams): Promise<WorkOrderPage> {
  const res = await get<PageResp<WorkOrder>>('/work-orders/my', {
    params: { status: params.status, pageNum: params.pageNum, pageSize: params.pageSize, keyword: params.keyword }
  })
  const records = res?.records ?? []
  const pageNum = res?.pageNum ?? 1
  const pages = res?.pages ?? 1
  return { list: records, total: res?.total ?? 0, hasMore: pageNum < pages }
}

export function getWorkOrderById(id: string | number): Promise<WorkOrder> {
  return get<WorkOrder>(`/work-orders/${id}`)
}

export function acceptWorkOrder(id: string | number): Promise<WorkOrder> {
  return post<WorkOrder>(`/work-orders/${id}/accept`)
}

/** Backend /start accepts no body — the coordinates live on the install record. */
export function startWorkOrder(id: string | number): Promise<WorkOrder> {
  return post<WorkOrder>(`/work-orders/${id}/start`)
}

/**
 * Backend transfer = return the order to the pool for re-dispatch
 * (POST /api/work-orders/{id}/transfer, body WorkOrderTransferReq{workOrderId, reason}).
 * There is no target-installer concept on the backend side.
 */
export function transferWorkOrder(id: string | number, reason: string): Promise<void> {
  return post<void>(`/work-orders/${id}/transfer`, { workOrderId: Number(id), reason })
}

/**
 * Backend /work-orders/{id}/complete reads a query param installRecordId and
 * returns R<Void>. The installer-facing completion path is /install/{id}/complete.
 */
export function completeWorkOrder(id: string | number, installRecordId?: string | number): Promise<void> {
  const qs = installRecordId != null ? `?installRecordId=${encodeURIComponent(String(installRecordId))}` : ''
  return post<void>(`/work-orders/${id}/complete${qs}`)
}
