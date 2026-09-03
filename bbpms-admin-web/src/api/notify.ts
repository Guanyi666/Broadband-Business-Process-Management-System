import request from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/common'

export interface NotifyTemplate {
  id: number | string
  code: string
  name?: string
  channel: 'SMS' | 'WECHAT' | 'INAPP'
  subject?: string
  content: string
  variables?: string
  status: 0 | 1
  updatedAt?: string
}

export interface NotifyMessage {
  id: number | string
  templateId?: number | string
  templateCode?: string
  channel: string
  receiver?: string
  userId?: number | string
  subject?: string
  content: string
  status: 'PENDING' | 'SUCCESS' | 'FAILED'
  errorMsg?: string
  sentAt?: string
}

interface PageResp<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
}

interface BackendTemplate extends Omit<NotifyTemplate, 'status' | 'updatedAt'> {
  enabled: 0 | 1
  updateTime?: string
}

export async function pageTemplates(params: PageQuery) {
  const res = await request<PageResp<BackendTemplate>>({
    url: '/notify/templates/page',
    method: 'GET',
    params
  })
  return {
    list: (res?.records ?? []).map((item) => ({ ...item, name: item.name || item.code, status: item.enabled, updatedAt: item.updateTime })),
    total: res?.total ?? 0,
    pageNum: res?.pageNum ?? 1,
    pageSize: res?.pageSize ?? 10
  } as PageResult<NotifyTemplate>
}

export function createTemplate(data: Partial<NotifyTemplate>) {
  return request<number | string>({ url: '/notify/templates', method: 'POST', data: { ...data, enabled: data.status } })
}

export function updateTemplate(id: number | string, data: Partial<NotifyTemplate>) {
  return request<void>({ url: `/notify/templates/${id}`, method: 'PUT', data: { ...data, enabled: data.status } })
}

export function deleteTemplate(id: number | string) {
  return request<void>({ url: `/notify/templates/${id}`, method: 'DELETE' })
}

export function sendSms(payload: { phone: string; templateCode: string; params?: Record<string, any> }) {
  return request<{ messageId: number | string; status: string }>({
    url: '/notify/sms',
    method: 'POST',
    data: payload
  })
}

export function sendWechat(payload: { openId: string; templateId: string; params?: Record<string, any>; url?: string }) {
  return request<{ messageId: number | string; status: string }>({
    url: '/notify/wechat/template',
    method: 'POST',
    data: payload
  })
}

export async function pageMessages(params: PageQuery & { channel?: string; status?: string }) {
  const res = await request<PageResp<any>>({
    url: '/notify/messages/page',
    method: 'GET',
    params
  })
  return {
    list: (res?.records ?? []).map((item: any) => ({
      ...item,
      receiver: item.userId ? `User #${item.userId}` : '-',
      sentAt: item.sendTime || item.createTime
    })),
    total: res?.total ?? 0,
    pageNum: res?.pageNum ?? 1,
    pageSize: res?.pageSize ?? 10
  } as PageResult<NotifyMessage>
}
