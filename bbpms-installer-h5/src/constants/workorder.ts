import type { WorkOrderStatus } from '@/types/workorder'

export type TagType = 'default' | 'primary' | 'success' | 'warning' | 'danger'

// Mirrors com.bbpms.common.enums.WorkOrderStatus (10 values).
export const WORK_ORDER_STATUS_LABEL: Record<WorkOrderStatus, string> = {
  PENDING: '待派发',
  DISPATCHED: '待接单',
  ACCEPTED: '已接单',
  IN_PROGRESS: '施工中',
  STALLED: '已停滞',
  REASSIGNING: '改派中',
  COMPLETED: '已完成',
  FAILED: '失败',
  CANCELLED: '已取消',
  AUTO_CANCELLED: '自动取消'
}

export const WORK_ORDER_STATUS_TAG: Record<WorkOrderStatus, TagType> = {
  PENDING: 'warning',
  DISPATCHED: 'warning',
  ACCEPTED: 'primary',
  IN_PROGRESS: 'primary',
  STALLED: 'danger',
  REASSIGNING: 'warning',
  COMPLETED: 'success',
  FAILED: 'danger',
  CANCELLED: 'default',
  AUTO_CANCELLED: 'default'
}

export function statusLabel(s: WorkOrderStatus): string {
  return WORK_ORDER_STATUS_LABEL[s] ?? s
}

export function statusType(s: WorkOrderStatus): TagType {
  return WORK_ORDER_STATUS_TAG[s] ?? 'default'
}
