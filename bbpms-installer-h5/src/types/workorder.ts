// Backend contract: com.bbpms.workorder.vo.WorkOrderVO (status = WorkOrderStatus enum).
// Aligned during Sprint A Phase 7/8 — H5 must consume exactly what the backend returns.

export type WorkOrderStatus =
  | 'PENDING'
  | 'DISPATCHED'
  | 'ACCEPTED'
  | 'IN_PROGRESS'
  | 'STALLED'
  | 'REASSIGNING'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED'
  | 'AUTO_CANCELLED'

export interface WorkOrder {
  id: number
  workNo: string
  orderId: number
  installerId: number | null
  installerName: string | null
  dispatcherId: number | null
  dispatcherName: string | null
  status: WorkOrderStatus
  statusDesc: string
  dispatchTime: string | null
  acceptTime: string | null
  startTime: string | null
  finishTime: string | null
  installAddress: string
  customerPhone: string
  packageName: string
  createTime: string | null
  updateTime: string | null
}

export interface WorkOrderListParams {
  status?: WorkOrderStatus
  pageNum: number
  pageSize: number
  keyword?: string
}

/**
 * Adapter shape produced by the API layer from the backend PageResp
 * ({records,total,pageNum,pageSize,pages}) so stores/views never see the wire format.
 */
export interface WorkOrderPage {
  list: WorkOrder[]
  total: number
  hasMore: boolean
}
