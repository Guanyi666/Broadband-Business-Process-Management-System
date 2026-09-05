import request from '@/utils/request'
import type { DispatchCandidate } from '@/types/order'
import type { DispatchStat } from '@/composables/useDashboard'

export interface DispatchResult {
  workOrderId: number | string
  workNo?: string
  installerId: number | string
  installerName?: string
  score?: number
}

/** Backend page envelope for /dispatch/records/page. */
interface PageResp<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

export function autoDispatch(orderId: number | string) {
  return request<DispatchResult>({
    url: '/dispatch/auto',
    method: 'POST',
    params: { orderId }
  })
}

export function manualDispatch(orderId: number | string, installerId: number | string, reason?: string) {
  return request<DispatchResult>({
    url: '/dispatch/manual',
    method: 'POST',
    data: { orderId, installerId, reason }
  })
}

/** Return a dispatched work order back to the pool for re-dispatch. */
export function reassignWorkorder(workorderId: number | string, reason?: string) {
  return request<DispatchResult>({
    url: `/dispatch/${workorderId}/reassign`,
    method: 'POST',
    data: { workOrderId: Number(workorderId), reason }
  })
}

/** Backend CandidateDTO (com.bbpms.dispatch.dto.CandidateDTO). */
interface CandidateDTO {
  installerId: number
  name: string
  username?: string
  phone?: string
  status?: 'AVAILABLE' | 'OFF_DUTY'
  distance?: number
  workload?: number
  skillMatchScore?: number
  rating?: number
  totalScore?: number
  factorBreakdown?: Record<string, number>
}

export async function dispatchCandidates(
  orderId: number | string,
  limit = 10,
  excludeInstallerId?: number | string
) {
  const list = await request<CandidateDTO[]>({
    url: '/dispatch/candidates',
    method: 'GET',
    params: { orderId, limit, excludeInstallerId }
  })
  // Adapter: CandidateDTO → DispatchCandidate (score/totalScore, distanceKm/distance).
  return (list || []).map((c) => ({
    installerId: c.installerId,
    name: c.name,
    username: c.username,
    phone: c.phone,
    status: c.status,
    distanceKm: c.distance,
    workload: c.workload,
    rating: c.rating,
    score: c.totalScore,
    online: c.status === 'AVAILABLE'
  })) as DispatchCandidate[]
}

export async function dispatchRecords(params: { orderId?: number | string; workorderId?: number | string; pageNum?: number; pageSize?: number }) {
  const res = await request<PageResp<Record<string, any>>>({
    url: '/dispatch/records/page',
    method: 'GET',
    params
  })
  return { list: res?.records ?? [], total: res?.total ?? 0 }
}

/** GET /dispatch/stats —— 派单策略构成与平均评分（后端 DispatchStatVO） */
export function getDispatchStats(days = 7) {
  return request<DispatchStat>({
    url: '/dispatch/stats',
    method: 'GET',
    params: { days }
  })
}
