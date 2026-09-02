import { defineStore } from 'pinia'
import { ref } from 'vue'
import { pageOrders, getOrderDetail, auditOrder, cancelOrder } from '@/api/order'
import type { OrderItem, OrderStatus } from '@/types/order'
import type { AuditPayload } from '@/api/order'

export const useOrderStore = defineStore('order', () => {
  const list = ref<OrderItem[]>([])
  const total = ref(0)
  const loading = ref(false)
  const query = ref<Record<string, any>>({ pageNum: 1, pageSize: 10 })

  async function fetchPage(extra?: Record<string, any>) {
    loading.value = true
    try {
      const params = { ...query.value, ...(extra || {}) }
      const res = await pageOrders(params)
      list.value = res.list
      total.value = res.total
    } finally {
      loading.value = false
    }
  }

  function setQuery(q: Record<string, any>) {
    query.value = { ...query.value, ...q }
  }

  function resetQuery() {
    query.value = { pageNum: 1, pageSize: 10 }
  }

  async function getDetail(id: number | string) {
    return await getOrderDetail(id)
  }

  async function audit(id: number | string, payload: AuditPayload) {
    await auditOrder(id, payload)
  }

  async function cancel(id: number | string, remark?: string) {
    await cancelOrder(id, remark)
  }

  return { list, total, loading, query, fetchPage, setQuery, resetQuery, getDetail, audit, cancel }
})

export type { OrderStatus }