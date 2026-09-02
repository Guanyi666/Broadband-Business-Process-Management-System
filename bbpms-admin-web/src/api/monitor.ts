import request from '@/utils/request'

export interface ServiceHealth {
  name: string
  status: 'UP' | 'DOWN' | 'WARN' | 'UNKNOWN'
  host?: string
  port?: number
  message?: string
  checkedAt: string
}

export interface SystemMetrics {
  cpu: number
  memory: number
  disk: number
  jvmHeapUsed?: number
  jvmHeapMax?: number
  threads?: number
  onlineInstallers?: number
  pendingOrders?: number
  runningWorkorders?: number
  timestamp: string
}

export function getMetrics() {
  return request<SystemMetrics>({
    url: '/monitor/metrics',
    method: 'GET'
  })
}

export function getServiceHealth() {
  return request<ServiceHealth[]>({
    url: '/monitor/health',
    method: 'GET'
  })
}