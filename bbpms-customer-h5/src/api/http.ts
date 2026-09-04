import axios, { type AxiosError, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { showFailToast } from 'vant'

interface ApiEnvelope<T = unknown> { code: number; msg?: string; message?: string; data: T }

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api', timeout: 30000,
  headers: { 'Content-Type': 'application/json;charset=UTF-8' }
})

http.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('bbpms_customer_token')
  if (token && config.headers) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use((response: AxiosResponse<ApiEnvelope>) => {
  const res = response.data
  if (res.code === 0 || res.code === 200) return res.data as unknown as AxiosResponse
  const msg = res.msg || res.message || '请求失败'
  showFailToast(msg)
  return Promise.reject(new Error(msg))
}, (error: AxiosError<ApiEnvelope>) => {
  const status = error.response?.status
  const msg = error.response?.data?.msg || error.response?.data?.message || error.message
  if (status === 401) {
    sessionStorage.removeItem('bbpms_customer_token')
    sessionStorage.removeItem('bbpms_customer_user')
    location.replace('/#/login')
  }
  showFailToast(status === 403 ? '当前账号无客户自助权限' : msg || '网络异常')
  return Promise.reject(error)
})

export const get = <T>(url: string, config?: AxiosRequestConfig) => http.get<T, T>(url, config) as Promise<T>
export const post = <T>(url: string, data?: unknown, config?: AxiosRequestConfig) => http.post<T, T>(url, data, config) as Promise<T>
export const put = <T>(url: string, data?: unknown, config?: AxiosRequestConfig) => http.put<T, T>(url, data, config) as Promise<T>
