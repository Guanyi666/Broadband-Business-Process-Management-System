import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as workorderApi from '@/api/workorder'
import type { WorkOrder, WorkOrderStatus } from '@/types/workorder'

interface TabState {
  list: WorkOrder[]
  page: number
  finished: boolean
  loading: boolean
}

// Server-side tab filters (backend /my filters by a single status).
export const TAB_KEYS = ['DISPATCHED', 'IN_PROGRESS', 'COMPLETED'] as const
export type TabKey = (typeof TAB_KEYS)[number]

function tabKeyFor(status: WorkOrderStatus): TabKey {
  if (status === 'DISPATCHED') return 'DISPATCHED'
  if (status === 'ACCEPTED' || status === 'IN_PROGRESS' || status === 'STALLED' || status === 'REASSIGNING') return 'IN_PROGRESS'
  return 'COMPLETED' // COMPLETED / CANCELLED / AUTO_CANCELLED / PENDING / FAILED
}

export const useWorkOrderStore = defineStore('workorder', () => {
  const currentWO = ref<WorkOrder | null>(null)
  const tabs = ref<Record<TabKey, TabState>>({
    DISPATCHED: { list: [], page: 0, finished: false, loading: false },
    IN_PROGRESS: { list: [], page: 0, finished: false, loading: false },
    COMPLETED: { list: [], page: 0, finished: false, loading: false }
  })

  async function fetchList(status: WorkOrderStatus, refresh = false): Promise<void> {
    const tabKey = tabKeyFor(status)
    const state = tabs.value[tabKey]
    if (state.loading) return
    if (!refresh && state.finished) return
    state.loading = true
    const page = refresh ? 1 : state.page + 1
    try {
      const res = await workorderApi.fetchMyWorkOrders({ status, pageNum: page, pageSize: 10 })
      if (refresh) state.list = res.list
      else state.list.push(...res.list)
      state.page = page
      state.finished = !res.hasMore
    } finally {
      state.loading = false
    }
  }

  async function fetchDetail(id: string | number): Promise<WorkOrder> {
    const wo = await workorderApi.getWorkOrderById(id)
    currentWO.value = wo
    return wo
  }

  function upsertLocal(wo: WorkOrder) {
    const tabKey = tabKeyFor(wo.status)
    const state = tabs.value[tabKey]
    const idx = state.list.findIndex((w) => w.id === wo.id)
    if (idx >= 0) state.list[idx] = wo
    else state.list.unshift(wo)
    if (currentWO.value?.id === wo.id) currentWO.value = wo
  }

  return { currentWO, tabs, fetchList, fetchDetail, upsertLocal }
})
