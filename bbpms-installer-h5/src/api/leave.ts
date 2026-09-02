import { get, post } from './http'

export type LeaveType = 'CASUAL' | 'ANNUAL' | 'SICK' | 'COMPASSIONATE' | 'UNPAID'

export interface LeaveVO {
  id: number
  applicantId: number
  applicantName?: string
  leaveType: LeaveType
  startAt: string
  endAt: string
  totalHours: number
  reason: string
  attachmentUrl?: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED'
  currentLevel: number
  requiredLevel: number
  level1ApproverName?: string
  level1DecidedAt?: string
  level1Remark?: string
  level2ApproverName?: string
  level2DecidedAt?: string
  level2Remark?: string
  appliedAt: string
}

export function applyLeave(payload: {
  leaveType: LeaveType
  startAt: string
  endAt: string
  reason: string
  attachmentUrl?: string
}) {
  return post<LeaveVO>('/leave/apply', payload)
}

export function cancelLeave(id: number) {
  return post<LeaveVO>(`/leave/${id}/cancel`, {})
}

export function fetchMyLeaves(status?: string) {
  return get<LeaveVO[]>('/leave/my', { params: status ? { status } : {} })
}

export function fetchLeaveDetail(id: number) {
  return get<LeaveVO>(`/leave/${id}`)
}
