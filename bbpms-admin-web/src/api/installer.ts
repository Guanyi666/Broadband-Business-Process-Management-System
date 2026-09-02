import request from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/common'
import type { InstallerLocation, InstallerProfile } from '@/types/installer'

/**
 * Backend page envelope (com.bbpms.common.result.PageResp) with
 * InstallerVO rows; adapted below to the admin PageResult shape.
 */
interface PageResp<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

/** Backend InstallerVO (com.bbpms.user.vo.InstallerVO). */
interface InstallerVO {
  userId: number
  username?: string
  realName?: string
  phone?: string
  onDuty?: number // 1 = on duty (online)
  lat?: number | null
  lng?: number | null
  workload?: number
  rating?: number | null
  lastActiveAt?: string
}

function toProfile(v: InstallerVO): InstallerProfile {
  return {
    id: v.userId,
    name: v.realName || v.username || String(v.userId),
    username: v.username,
    phone: v.phone,
    status: v.onDuty === 1 ? 'WORKING' : 'OFFLINE',
    workload: v.workload,
    rating: v.rating ?? undefined,
    lastActiveAt: v.lastActiveAt
  }
}

function toLocation(v: InstallerVO): InstallerLocation {
  return {
    installerId: v.userId,
    name: v.realName || v.username || String(v.userId),
    phone: v.phone,
    lng: Number(v.lng ?? 0),
    lat: Number(v.lat ?? 0),
    online: v.onDuty === 1,
    workload: v.workload,
    rating: v.rating ?? undefined,
    updatedAt: v.lastActiveAt
  }
}

export function listInstallerLocations() {
  return request<InstallerVO[]>({ url: '/installers/locations', method: 'GET' }).then((list) => (list || []).map(toLocation))
}

export function getInstallerProfile(id: number | string) {
  return request<InstallerVO>({ url: `/installers/${id}/profile`, method: 'GET' }).then((vo) => (vo ? toProfile(vo) : null))
}

export async function pageInstallers(params?: PageQuery) {
  const res = await request<PageResp<InstallerVO>>({
    url: '/installers/page',
    method: 'GET',
    params
  })
  return {
    list: (res?.records ?? []).map(toProfile),
    total: res?.total ?? 0,
    pageNum: res?.pageNum ?? 1,
    pageSize: res?.pageSize ?? 10
  } as PageResult<InstallerProfile>
}
