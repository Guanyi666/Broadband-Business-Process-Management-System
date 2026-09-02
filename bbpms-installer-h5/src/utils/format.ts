import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import relativeTime from 'dayjs/plugin/relativeTime'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

export function formatDate(input: number | string | Date | null | undefined, pattern = 'YYYY-MM-DD HH:mm'): string {
  if (!input) return '--'
  return dayjs(input).isValid() ? dayjs(input).format(pattern) : '--'
}

export function formatRelative(input: number | string | Date): string {
  if (!input) return '--'
  return dayjs(input).fromNow()
}

export function formatDistance(meters?: number | null): string {
  if (meters == null) return '--'
  if (meters < 1000) return `${meters} m`
  return `${(meters / 1000).toFixed(2)} km`
}

export function maskPhone(phone?: string): string {
  if (!phone || phone.length < 7) return phone || ''
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}
