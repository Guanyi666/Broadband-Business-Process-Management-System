export interface OperationLog {
  id: number | string
  module: string
  action: string
  method?: string
  requestUrl?: string
  operatorId?: number | string
  operatorName?: string
  ip?: string
  params?: string
  result?: string
  status: 'SUCCESS' | 'FAILED'
  durationMs?: number
  createdAt: string
}

export interface LoginLog {
  id: number | string
  username: string
  ip?: string
  userAgent?: string
  location?: string
  terminal?: string
  status: 'SUCCESS' | 'FAILED'
  message?: string
  createdAt: string
}
