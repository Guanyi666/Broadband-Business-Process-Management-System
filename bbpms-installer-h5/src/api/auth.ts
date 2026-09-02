import { get, post } from './http'
import type { LoginParams, LoginResult, CaptchaResult, UserInfo } from '@/types/auth'

export function fetchCaptcha(): Promise<CaptchaResult> {
  return get<CaptchaResult>('/auth/captcha')
}
// Backend /auth/public-key returns the raw PEM string inside R.data (no wrapper object).
export function fetchPublicKey(): Promise<string> {
  return get<string>('/auth/public-key')
}
export function login(params: LoginParams): Promise<LoginResult> {
  return post<LoginResult>('/auth/login', params)
}
export function logout(): Promise<void> {
  return post<void>('/auth/logout')
}
export function fetchProfile(): Promise<UserInfo> {
  return get<UserInfo>('/auth/me')
}
