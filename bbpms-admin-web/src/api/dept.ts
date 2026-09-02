import request from '@/utils/request'
import type { DeptNode } from '@/types/auth'

export function listDeptTree(params?: { name?: string; status?: number }) {
  return request<DeptNode[]>({ url: '/depts/tree', method: 'GET', params })
}

export function createDept(data: Partial<DeptNode>) {
  return request<DeptNode>({ url: '/depts', method: 'POST', data })
}

export function updateDept(id: number | string, data: Partial<DeptNode>) {
  return request<void>({ url: `/depts/${id}`, method: 'PUT', data })
}

export function deleteDept(id: number | string) {
  return request<void>({ url: `/depts/${id}`, method: 'DELETE' })
}