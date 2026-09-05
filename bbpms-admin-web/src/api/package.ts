import request from '@/utils/request'
import type { PageResult } from '@/types/common'

/** 套餐资源项 */
export interface PackageItem {
  id: number
  code: string
  name: string
  nameEn?: string
  speedMbps: number
  monthlyFee: number
  description?: string
  status: number
  sort: number
  createTime?: string
}

/** 新增/编辑请求 */
export interface PackageSavePayload {
  code: string
  name: string
  nameEn?: string
  speedMbps: number
  monthlyFee: number
  description?: string
  status: number
  sort?: number
}

interface PageResp<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
}

export async function pagePackages(params: Record<string, unknown>) {
  const res = await request<PageResp<PackageItem>>({
    url: '/packages',
    method: 'GET',
    params
  })
  return {
    list: res?.records ?? [],
    total: res?.total ?? 0,
    pageNum: res?.pageNum ?? 1,
    pageSize: res?.pageSize ?? 10
  } as PageResult<PackageItem>
}

export function createPackage(data: PackageSavePayload) {
  return request<number>({ url: '/packages', method: 'POST', data })
}

export function updatePackage(id: number, data: PackageSavePayload) {
  return request<void>({ url: `/packages/${id}`, method: 'PUT', data })
}

export function deletePackage(id: number) {
  return request<void>({ url: `/packages/${id}`, method: 'DELETE' })
}

export function togglePackageStatus(id: number, status: number) {
  return request<void>({ url: `/packages/${id}/status`, method: 'PUT', params: { status } })
}
