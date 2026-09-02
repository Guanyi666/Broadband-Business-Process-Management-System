import { get, post } from './http'

export interface AttendanceVO {
  id: number
  installerId: number
  installerName?: string
  workDate: string
  clockInAt?: string
  clockOutAt?: string
  workMinutes?: number
  breakMinutes?: number
  status: 'OFF_DUTY' | 'ON_DUTY' | 'ON_BREAK' | 'AUTO_OFF'
  source: 'MANUAL' | 'GPS' | 'SCHEDULED' | 'HEARTBEAT_TIMEOUT'
  locationLat?: number
  locationLng?: number
  remark?: string
  isLate?: boolean
  isEarlyLeave?: boolean
}

export interface AttendanceSummaryVO {
  installerId: number
  yearMonth: string
  totalWorkMinutes: number
  workDays: number
  lateCount: number
  earlyLeaveCount: number
  absentCount: number
}

export function clockIn(payload: { lat?: number; lng?: number; source?: string; remark?: string }) {
  return post<AttendanceVO>('/attendance/clock-in', { source: 'GPS', ...payload })
}

export function clockOut(payload: { lat?: number; lng?: number; remark?: string }) {
  return post<AttendanceVO>('/attendance/clock-out', payload || {})
}

export function breakStart() {
  return post<AttendanceVO>('/attendance/break/start', {})
}

export function breakEnd() {
  return post<AttendanceVO>('/attendance/break/end', {})
}

export function fetchToday() {
  return get<AttendanceVO>('/attendance/today')
}

export function fetchMy(limit = 30) {
  return get<AttendanceVO[]>('/attendance/my', { params: { limit } })
}

export function fetchMonthly(month?: string) {
  return get<AttendanceSummaryVO>('/attendance/monthly', { params: month ? { month } : {} })
}
