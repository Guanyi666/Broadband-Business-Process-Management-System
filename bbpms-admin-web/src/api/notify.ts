import request from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/common'

export interface NotifyTemplate {
  id: number | string
  code: string
  name: string
  channel: 'SMS' | 'WECHAT' | 'APP_PUSH'
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
  receiver: string
  subject?: string
  content: string
  status: 'PENDING' | 'SUCCESS' | 'FAILED'
  errorMsg?: string
  sentAt?: string
}

export function pageTemplates(params: PageQuery) {
  return request<PageResult<NotifyTemplate>>({
    url: '/notify/templates/page',
    method: 'GET',
    params
  })
}

export function createTemplate(data: Partial<NotifyTemplate>) {
  return request<NotifyTemplate>({ url: '/notify/templates', method: 'POST', data })
}

export function updateTemplate(id: number | string, data: Partial<NotifyTemplate>) {
  return request<void>({ url: `/notify/templates/${id}`, method: 'PUT', data })
}

export function deleteTemplate(id: number | string) {
  return request<void>({ url: `/notify/templates/${id}`, method: 'DELETE' })
}

export function sendSms(payload: { phones: string[]; templateCode?: string; content?: string; params?: Record<string, any> }) {
  return request<{ success: number; failed: number }>({
    url: '/notify/sms/send',
    method: 'POST',
    data: payload
  })
}

export function sendWechat(payload: { openIds?: string[]; userIds?: string[]; templateCode?: string; content?: string }) {
  return request<{ success: number; failed: number }>({
    url: '/notify/wechat/send',
    method: 'POST',
    data: payload
  })
}

export function pageMessages(params: PageQuery & { channel?: string; status?: string }) {
  return request<PageResult<NotifyMessage>>({
    url: '/notify/messages/page',
    method: 'GET',
    params
  })
}