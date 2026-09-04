import request from '@/utils/request'

export interface PageResp<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

export interface CustomerAccount {
  customerId: number | string
  userId: number | string
  username: string
  nickname?: string
  status: number
  bindTime?: string
  lastLoginTime?: string
}

export interface CustomerOrder {
  id: number | string
  orderNo: string
  packageName: string
  installAddress: string
  status: string
  statusLabel: string
  resourceStatus?: string
  appointmentTime?: string
  createTime?: string
}

export interface ServiceTicket {
  id: number | string
  ticketNo: string
  customerId: number | string
  orderId?: number | string
  type: 'REPAIR' | 'COMPLAINT'
  category?: string
  description: string
  status: string
  handleResult?: string
  handlerId?: number | string
  createTime?: string
}

export interface ProfileChange {
  id: number | string
  customerId: number | string
  changeFields: string
  proposedName?: string
  proposedPhone?: string
  proposedIdCardNo?: string
  proposedAddress?: string
  status: string
  reviewRemark?: string
  createTime?: string
}

export function getCustomerAccount(customerId: number | string) {
  return request<CustomerAccount | null>({
    url: `/customer-portal/admin/customers/${customerId}/account`, method: 'GET'
  })
}

export function openCustomerAccount(data: { customerId: number | string; username: string; password: string }) {
  return request<CustomerAccount>({ url: '/customer-portal/admin/accounts', method: 'POST', data })
}

export function setCustomerAccountStatus(customerId: number | string, status: number) {
  return request<CustomerAccount>({
    url: `/customer-portal/admin/customers/${customerId}/account/status`, method: 'PUT', data: { status }
  })
}

export function resetCustomerPassword(customerId: number | string, newPassword: string) {
  return request<void>({
    url: `/customer-portal/admin/customers/${customerId}/account/password`, method: 'POST', data: { newPassword }
  })
}

export function pageCustomerSubmittedOrders(params: Record<string, unknown>) {
  return request<PageResp<CustomerOrder>>({ url: '/customer-portal/admin/orders', method: 'GET', params })
}

export function reviewCustomerOrder(id: number | string, approved: boolean, remark?: string) {
  return request<void>({ url: `/customer-portal/admin/orders/${id}/review`, method: 'POST', data: { approved, remark } })
}

export function pageServiceTickets(params: Record<string, unknown>) {
  return request<PageResp<ServiceTicket>>({ url: '/customer-portal/admin/tickets', method: 'GET', params })
}

export function handleServiceTicket(id: number | string, data: { status: string; handleResult?: string }) {
  return request<void>({ url: `/customer-portal/admin/tickets/${id}`, method: 'PUT', data })
}

export function pageProfileChanges(params: Record<string, unknown>) {
  return request<PageResp<ProfileChange>>({ url: '/customer-portal/admin/profile-changes', method: 'GET', params })
}

export function reviewProfileChange(id: number | string, approved: boolean, remark?: string) {
  return request<void>({
    url: `/customer-portal/admin/profile-changes/${id}/review`, method: 'POST', data: { approved, remark }
  })
}
