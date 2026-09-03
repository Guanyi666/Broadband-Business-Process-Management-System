import request from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/common'

/** Backend CustomerVO (com.bbpms.order.vo.CustomerVO); PII masked unless privileged. */
export interface Customer {
  id: number | string
  name: string
  phone: string
  idCardNo?: string
  address?: string
  province?: string
  city?: string
  district?: string
  lng?: number
  lat?: number
  gridCode?: string
  masked?: boolean
  createTime?: string
}

/** Backend page envelope (com.bbpms.common.result.PageResp). */
interface PageResp<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

export async function pageCustomers(params: PageQuery) {
  const res = await request<PageResp<Customer>>({
    url: '/customers/page',
    method: 'GET',
    params
  })
  return {
    list: res?.records ?? [],
    total: res?.total ?? 0,
    pageNum: res?.pageNum ?? 1,
    pageSize: res?.pageSize ?? 10
  } as PageResult<Customer>
}

export function getCustomer(id: number | string) {
  return request<Customer>({
    url: `/customers/${id}`,
    method: 'GET'
  })
}

export function searchCustomers(keyword: string, limit = 20) {
  return request<Customer[]>({
    url: '/customers/search',
    method: 'GET',
    params: { keyword, limit }
  })
}

export function createCustomer(data: Pick<Customer, 'name' | 'phone'> & Partial<Customer>) {
  return request<number | string>({ url: '/customers', method: 'POST', data })
}
