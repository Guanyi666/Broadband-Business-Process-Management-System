import { defineStore } from 'pinia'
import { ref } from 'vue'
import { pageWorkorders } from '@/api/workorder'
import type { WorkorderItem } from '@/types/order'

export const useWorkorderStore = defineStore('workorder', () => {
  const list = ref<WorkorderItem[]>([])
  const total = ref(0)
  const loading = ref(false)
  const query = ref<Record<string, any>>({ pageNum: 1, pageSize: 10 })

  async function fetchPage(extra?: Record<string, any>) {
    loading.value = true
    try {
      const params = { ...query.value, ...(extra || {}) }
      const res = await pageWorkorders(params)
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

  return { list, total, loading, query, fetchPage, setQuery, resetQuery }
})