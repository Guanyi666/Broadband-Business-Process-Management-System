import request from '@/utils/request'

export interface LeaveVO {
  id: number
  applicantId: number
  applicantName?: string
  leaveType: string
  startAt: string
  endAt: string
  totalHours: number
  reason: string
  attachmentUrl?: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED'
  currentLevel: number
  requiredLevel: number
  level1ApproverName?: string
  level2ApproverName?: string
  level1DecidedAt?: string
  level2DecidedAt?: string
  appliedAt: string
}

export function fetchPendingApprovals(level?: number) {
  return request<LeaveVO[]>({
    url: '/leave/pending-approvals',
    method: 'GET',
    params: level ? { level } : {}
  })
}

export function approveLeave(id: number, payload: { approvalLevel: number; action: 'APPROVE' | 'REJECT'; comment?: string }) {
  return request<LeaveVO>({ url: `/leave/${id}/approve`, method: 'POST', data: payload })
}

export function cancelLeave(id: number) {
  return request<LeaveVO>({ url: `/leave/${id}/cancel`, method: 'POST' })
}

export function fetchTeamCalendar(from: string, to: string) {
  return request<LeaveVO[]>({ url: '/leave/team-calendar', method: 'GET', params: { from, to } })
}
