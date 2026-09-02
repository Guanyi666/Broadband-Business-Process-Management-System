import request from '@/utils/request'
import type { MenuNode } from '@/types/auth'

export function listMenuTree() {
  return request<MenuNode[]>({ url: '/menus/tree', method: 'GET' })
}

export function createMenu(data: Partial<MenuNode>) {
  return request<MenuNode>({ url: '/menus', method: 'POST', data })
}

export function updateMenu(id: number | string, data: Partial<MenuNode>) {
  return request<void>({ url: `/menus/${id}`, method: 'PUT', data })
}

export function deleteMenu(id: number | string) {
  return request<void>({ url: `/menus/${id}`, method: 'DELETE' })
}

export function listAllPerms() {
  return request<string[]>({ url: '/menus/perms', method: 'GET' })
}