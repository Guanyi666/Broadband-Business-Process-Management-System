import request from '@/utils/request'

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
  source: string
  isLate?: boolean
}

export function fetchOnDuty() {
  return request<AttendanceVO[]>({ url: '/attendance/on-duty', method: 'GET' })
}

export function fetchInstallerDaily(installerId: number, limit = 30) {
  return request<AttendanceVO[]>({
    url: `/attendance/daily/${installerId}`,
    method: 'GET',
    params: { limit }
  })
}

export function fetchInstallerMonthly(installerId: number, month?: string) {
  return request<any>({
    url: `/attendance/monthly/${installerId}`,
    method: 'GET',
    params: month ? { month } : {}
  })
}
