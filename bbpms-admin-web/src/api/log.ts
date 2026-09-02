import request from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/common'
import type { OperationLog, LoginLog } from '@/types/log'

/** Backend page envelope (com.bbpms.common.result.PageResp). */
interface PageResp<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

async function adapt<T>(p: Promise<PageResp<T> | null>): Promise<PageResult<T>> {
  const res = await p
  return { list: res?.records ?? [], total: res?.total ?? 0, pageNum: res?.pageNum ?? 1, pageSize: res?.pageSize ?? 10 }
}

export function pageOperationLogs(params: PageQuery & { module?: string; status?: string; dateRange?: string[] }) {
  return adapt(
    request<PageResp<OperationLog>>({
      url: '/logs/operation/page',
      method: 'GET',
      params
    })
  )
}

export function pageLoginLogs(params: PageQuery & { status?: string; username?: string; dateRange?: string[] }) {
  return adapt(
    request<PageResp<LoginLog>>({
      url: '/logs/login/page',
      method: 'GET',
      params
    })
  )
}
