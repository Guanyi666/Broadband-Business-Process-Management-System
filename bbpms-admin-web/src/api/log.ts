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

function terminalFromUserAgent(userAgent?: string) {
  if (!userAgent) return 'Unknown'
  const os = /Android/i.test(userAgent) ? 'Android' : /iPhone|iPad/i.test(userAgent) ? 'iOS' : /Windows/i.test(userAgent) ? 'Windows' : /Mac OS/i.test(userAgent) ? 'macOS' : 'Other'
  const browser = /Edg\//i.test(userAgent) ? 'Edge' : /Chrome\//i.test(userAgent) ? 'Chrome' : /Firefox\//i.test(userAgent) ? 'Firefox' : /Safari\//i.test(userAgent) ? 'Safari' : 'Browser'
  return `${os} / ${browser}`
}

export async function pageLoginLogs(params: PageQuery & { status?: string; username?: string; startTime?: string; endTime?: string }) {
  const backendParams = { ...params, status: params.status === 'SUCCESS' ? 1 : params.status === 'FAILED' ? 0 : undefined }
  const res = await request<PageResp<any>>({
      url: '/logs/login/page',
      method: 'GET',
      params: backendParams
    })
  return {
    list: (res?.records ?? []).map((item: any) => ({
      ...item,
      status: item.status === 1 ? 'SUCCESS' : 'FAILED',
      createdAt: item.createTime,
      location: item.ip === '127.0.0.1' || item.ip === '::1' ? 'Local machine' : 'Unknown',
      terminal: terminalFromUserAgent(item.userAgent)
    })),
    total: res?.total ?? 0,
    pageNum: res?.pageNum ?? 1,
    pageSize: res?.pageSize ?? 10
  } as PageResult<LoginLog>
}
