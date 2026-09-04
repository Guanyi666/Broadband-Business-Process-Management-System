import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig
} from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from './auth'
import router from '@/router'

export interface ApiResponse<T = unknown> {
  code: number
  msg?: string
  message?: string
  data: T
  success: boolean
}

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 20000,
  withCredentials: true
})

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data
    // file blob passthrough
    if (response.config.responseType === 'blob') return response

    if (res && typeof res === 'object' && 'code' in res) {
      if (res.code === 0 || res.code === 200 || res.success) {
        return res.data as any
      }
      if (res.code === 401) {
        handleUnauthorized()
        return Promise.reject(new Error(res.msg || res.message || 'Unauthenticated'))
      }
      const message = res.msg || res.message || 'Request failed'
      ElMessage.error(message)
      return Promise.reject(new Error(message))
    }
    return res as any
  },
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      handleUnauthorized()
    } else if (status === 403) {
      // 403 = 已认证但无权限。页面级权限由路由守卫拦截（跳 403 页），
      // 到达这里的 403 多为模块/接口级权限不足（如看板内某模块）。
      // 静默 reject，由调用方决定是否提示 —— 避免无权限用户看到全局报错弹窗。
      // 需要提示的调用方可在 catch 中自行处理（见 useDashboard 的 403 静默降级）。
      return Promise.reject(error)
    } else if (status === 500) {
      ElMessage.error('服务器暂时无法处理请求，请稍后重试')
    } else if (!error.response) {
      ElMessage.error('网络连接异常，请检查网络后重试')
    } else {
      const msg = error?.response?.data?.msg || error?.response?.data?.message || `请求失败（HTTP ${status}）`
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

function handleUnauthorized() {
  removeToken()
  ElMessage.error('登录已过期，请重新登录')
  const cur = router.currentRoute.value.fullPath
  router.replace({ path: '/login', query: { redirect: cur } })
}

export function request<T = unknown>(config: AxiosRequestConfig): Promise<T> {
  return service.request<unknown, T>(config)
}

export default service
