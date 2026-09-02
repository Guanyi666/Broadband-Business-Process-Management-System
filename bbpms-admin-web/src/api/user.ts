import request from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/common'

export interface SysUser {
  id: number | string
  username: string
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
  deptId?: number | string
  deptName?: string
  status?: number
  roles?: { id: number | string; name: string }[]
  createdAt?: string
}

export interface UserCreatePayload {
  username: string
  password: string
  nickname?: string
  email?: string
  phone?: string
  deptId?: number | string
  roleIds: (number | string)[]
  status?: number
}

export function pageUsers(params: PageQuery & { deptId?: number | string; status?: number }) {
  return request<PageResult<SysUser>>({
    url: '/users/page',
    method: 'GET',
    params
  })
}

export function createUser(data: UserCreatePayload) {
  return request<SysUser>({ url: '/users', method: 'POST', data })
}

export function updateUser(id: number | string, data: Partial<UserCreatePayload>) {
  return request<void>({ url: `/users/${id}`, method: 'PUT', data })
}

export function deleteUser(id: number | string) {
  return request<void>({ url: `/users/${id}`, method: 'DELETE' })
}

export function assignRoles(userId: number | string, roleIds: (number | string)[]) {
  return request<void>({
    url: `/users/${userId}/roles`,
    method: 'PUT',
    data: { roleIds }
  })
}

// Backend: POST /users/{id}/password with { oldPassword, newPassword }.
export function changePassword(userId: number | string, payload: { oldPassword: string; newPassword: string }) {
  return request<void>({ url: `/users/${userId}/password`, method: 'POST', data: payload })
}