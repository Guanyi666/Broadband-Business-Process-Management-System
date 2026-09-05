import { get, post, put } from './http'
import type {
  CustomerTrack, MessageItem, OrderDetail, OrderSummary, PackageInfo, PageResp, Profile, ProfileChange,
  ResourceCheckResult, ServiceTicket, TimelineItem
} from '@/types'

export const getProfile = () => get<Profile>('/customer-portal/profile')
export const changePassword = (data: { oldPassword: string; newPassword: string }) => post<void>('/customer-portal/password', data)
export const listPackages = () => get<PackageInfo[]>('/customer-portal/packages')
export const checkResource = (data: { address: string; roomNo?: string }) => post<ResourceCheckResult>('/customer-portal/resources/check', data)
export const pageOrders = (params: Record<string, unknown> = {}) => get<PageResp<OrderSummary>>('/customer-portal/orders', { params })
export const getOrder = (id: number | string) => get<OrderDetail>(`/customer-portal/orders/${id}`)
export const getTimeline = (id: number | string) => get<TimelineItem[]>(`/customer-portal/orders/${id}/timeline`)
/** 履约进度（5 节点骨架 + 催单状态） */
export const getCustomerTrack = (id: number | string) => get<CustomerTrack>(`/customer-portal/orders/${id}/track`)
/** 超时催单 */
export const urgeOrder = (id: number | string) => post<void>(`/customer-portal/orders/${id}/urge`)
export const createOrder = (data: Record<string, unknown>) => post<number | string>('/customer-portal/orders', data)
export const resubmitOrder = (id: number | string, data: Record<string, unknown>) => put<void>(`/customer-portal/orders/${id}/resubmit`, data)
export const reschedule = (id: number | string, data: { appointmentTime: string; reason?: string }) => put<void>(`/customer-portal/orders/${id}/appointment`, data)
export const pageTickets = (params: Record<string, unknown> = {}) => get<PageResp<ServiceTicket>>('/customer-portal/tickets', { params })
export const getTicket = (id: number | string) => get<ServiceTicket>(`/customer-portal/tickets/${id}`)
export const createTicket = (data: Record<string, unknown>) => post<number | string>('/customer-portal/tickets', data)
export const confirmTicket = (id: number | string) => post<void>(`/customer-portal/tickets/${id}/confirm`)
export const getEvaluation = (id: number | string) => get<Record<string, unknown> | null>(`/customer-portal/orders/${id}/evaluation`)
export const evaluateOrder = (id: number | string, data: Record<string, unknown>) => post<number | string>(`/customer-portal/orders/${id}/evaluation`, data)
export const requestProfileChange = (data: Record<string, unknown>) => post<number | string>('/customer-portal/profile/change-requests', data)
export const listProfileChanges = () => get<ProfileChange[]>('/customer-portal/profile/change-requests')
export const pageMessages = () => get<PageResp<MessageItem>>('/customer-portal/messages')
export const readMessage = (id: number | string) => put<void>(`/customer-portal/messages/${id}/read`)
