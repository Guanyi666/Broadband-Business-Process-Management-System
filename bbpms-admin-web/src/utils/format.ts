import dayjs from 'dayjs'

export function formatDate(d?: string | number | Date | null, fmt = 'YYYY-MM-DD HH:mm:ss'): string {
  if (!d) return '-'
  return dayjs(d).format(fmt)
}

export function formatDateOnly(d?: string | number | Date | null): string {
  return formatDate(d, 'YYYY-MM-DD')
}

export function formatTimeOnly(d?: string | number | Date | null): string {
  return formatDate(d, 'HH:mm')
}

export function fromNow(d?: string | number | Date | null): string {
  if (!d) return '-'
  const diff = Date.now() - new Date(d).getTime()
  const m = Math.floor(diff / 60000)
  if (m < 1) return 'just now'
  if (m < 60) return `${m}m ago`
  const h = Math.floor(m / 60)
  if (h < 24) return `${h}h ago`
  const days = Math.floor(h / 24)
  return `${days}d ago`
}

export function maskPhone(phone?: string | null): string {
  if (!phone || phone.length < 7) return phone || '-'
  return phone.replace(/^(\d{3})\d{4}(\d+)$/, '$1****$2')
}

export function maskIdCard(id?: string | null): string {
  if (!id || id.length < 8) return id || '-'
  return id.replace(/^(.{4}).+(.{4})$/, '$1**********$2')
}

export function maskName(name?: string | null): string {
  if (!name) return '-'
  if (name.length <= 1) return name
  if (name.length === 2) return name[0] + '*'
  return name[0] + '*'.repeat(name.length - 2) + name[name.length - 1]
}

export function formatCurrency(n?: number | null): string {
  if (n === null || n === undefined) return '-'
  return `¥${Number(n).toFixed(2)}`
}