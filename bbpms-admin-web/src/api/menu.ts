import request from '@/utils/request'
import type { MenuNode } from '@/types/auth'

export function listMenuTree() {
  return request<MenuNode[]>({ url: '/menus/tree', method: 'GET' })
}

export function createMenu(data: Partial<MenuNode>) {
  return request<number | string>({ url: '/menus', method: 'POST', data: toBackendMenu(data) })
}

export function updateMenu(id: number | string, data: Partial<MenuNode>) {
  return request<void>({ url: `/menus/${id}`, method: 'PUT', data: toBackendMenu(data) })
}

function toBackendMenu(data: Partial<MenuNode>) {
  return {
    parentId: data.parentId,
    menuName: data.name,
    menuType: data.type == null ? undefined : String(data.type),
    path: data.path,
    component: data.component,
    perms: data.perm,
    icon: data.icon,
    sortOrder: data.sort,
    visible: data.visible === false ? 0 : 1,
    status: data.status
  }
}

export function deleteMenu(id: number | string) {
  return request<void>({ url: `/menus/${id}`, method: 'DELETE' })
}

export function listAllPerms() {
  return request<string[]>({ url: '/menus/perms', method: 'GET' })
}
