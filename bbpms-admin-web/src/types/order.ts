export type OrderStatus = 'PENDING' | 'AUDIT_PASS' | 'AUDIT_REJECT' | 'DISPATCHED' | 'INSTALLING' | 'DONE' | 'CANCELLED'

export interface OrderItem {
  id: number | string
  orderNo: string
  status: OrderStatus
  customerId: number | string
  customerName?: string
  customerPhone?: string
  packageId?: number | string
  packageName?: string
  address?: string
  appointmentAt?: string
  csId?: number | string
  csName?: string
  auditBy?: number | string
  auditByName?: string
  auditAt?: string
  remark?: string
  createdAt: string
  updatedAt?: string
}

export interface OrderTimelineItem {
  id: number | string
  orderId: number | string
  action: string
  operator?: string
  operatorName?: string
  remark?: string
  createdAt: string
}

// Mirrors com.bbpms.workorder.vo.WorkOrderVO (aligned in Sprint A Phase 8).
export type WorkorderStatus =
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

export interface WorkorderItem {
  id: number | string
  workNo: string
  orderId: number | string
  installerId?: number | null
  installerName?: string | null
  dispatcherId?: number | null
  dispatcherName?: string | null
  status: WorkorderStatus
  statusDesc?: string
  dispatchTime?: string | null
  acceptTime?: string | null
  startTime?: string | null
  finishTime?: string | null
  installAddress?: string
  customerPhone?: string
  packageName?: string
  createTime?: string | null
  updateTime?: string | null
}

export interface DispatchCandidate {
  installerId: number | string
  name: string
  phone?: string
  distanceKm?: number
  workload?: number
  rating?: number
  score?: number
  online?: boolean
  lng?: number
  lat?: number
  reasons?: string[]
}