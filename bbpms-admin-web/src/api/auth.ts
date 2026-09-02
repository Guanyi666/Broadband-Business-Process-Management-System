import request from '@/utils/request'
import type { LoginPayload, LoginResult, CaptchaResult } from '@/types/auth'

export function login(data: LoginPayload) {
  return request<LoginResult>({
    url: '/auth/login',
    method: 'POST',
    data
  })
}

export function refreshToken(refreshToken: string) {
  return request<LoginResult>({
    url: '/auth/refresh',
    method: 'POST',
    data: { refreshToken }
  })
}

export function logout() {
  return request<void>({
    url: '/auth/logout',
    method: 'POST'
  })
}

export function getCaptcha() {
  return request<CaptchaResult>({
    url: '/auth/captcha',
    method: 'GET'
  })
}

export function getPublicKey() {
  return request<string>({
    url: '/auth/public-key',
    method: 'GET'
  })
}

export function getCurrentUser() {
  return request<import('@/types/auth').UserInfo>({
    url: '/auth/me',
    method: 'GET'
  })
}

export function getUserMenus() {
  return request<import('@/types/auth').MenuNode[]>({
    url: '/auth/menus',
    method: 'GET'
  })
}