import request from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/common'
import type { RoleInfo } from '@/types/auth'

/** Backend page envelope (com.bbpms.common.result.PageResp). */
interface PageResp<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

export async function pageRoles(params: PageQuery) {
  const res = await request<PageResp<RoleInfo>>({
    url: '/roles/page',
    method: 'GET',
    params
  })
  return {
    list: res?.records ?? [],
    total: res?.total ?? 0,
    pageNum: res?.pageNum ?? 1,
    pageSize: res?.pageSize ?? 10
  } as PageResult<RoleInfo>
}

export function listRoles() {
  return request<RoleInfo[]>({ url: '/roles', method: 'GET' })
}

export function createRole(data: Partial<RoleInfo>) {
  return request<RoleInfo>({ url: '/roles', method: 'POST', data })
}

export function updateRole(id: number | string, data: Partial<RoleInfo>) {
  return request<void>({ url: '/roles', method: 'PUT', data: { ...data, id } })
}

export function deleteRole(id: number | string) {
  return request<void>({ url: `/roles/${id}`, method: 'DELETE' })
}

// Backend: POST /roles/{id}/menus with a bare List<Long> body (not {menuIds}).
export function assignMenus(roleId: number | string, menuIds: (number | string)[]) {
  return request<void>({
    url: `/roles/${roleId}/menus`,
    method: 'POST',
    data: menuIds
  })
}
