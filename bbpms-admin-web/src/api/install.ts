import request from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/common'

export interface InstallRecord {
  id: number | string
  workorderId: number | string
  workorderNo?: string
  installerId: number | string
  installerName?: string
  customerName?: string
  address?: string
  photos?: string[]
  signature?: string
  remark?: string
  startedAt?: string
  completedAt?: string
  status?: string
}

/** Backend page envelope (com.bbpms.common.result.PageResp). */
interface PageResp<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

export async function pageInstallRecords(params: PageQuery & { installerId?: number | string; status?: string }) {
  const res = await request<PageResp<InstallRecord>>({
    url: '/install/page',
    method: 'GET',
    params
  })
  return {
    list: res?.records ?? [],
    total: res?.total ?? 0,
    pageNum: res?.pageNum ?? 1,
    pageSize: res?.pageSize ?? 10
  } as PageResult<InstallRecord>
}

export function uploadInstallPhoto(workorderId: number | string, form: FormData) {
  return request<{ url: string; id: number | string }>({
    url: `/install/${workorderId}/photos`,
    method: 'POST',
    data: form,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
