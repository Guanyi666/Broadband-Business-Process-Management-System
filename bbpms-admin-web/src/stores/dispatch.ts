import { defineStore } from 'pinia'
import { ref } from 'vue'
import { autoDispatch, manualDispatch, dispatchCandidates } from '@/api/dispatch'
import type { DispatchCandidate } from '@/types/order'

export const useDispatchStore = defineStore('dispatch', () => {
  const candidates = ref<Record<string, DispatchCandidate[]>>({})
  const lastResult = ref<Record<string, any>>({})

  async function fetchCandidates(orderId: number | string) {
    const data = await dispatchCandidates(orderId)
    candidates.value[orderId] = data
    return data
  }

  async function auto(orderId: number | string) {
    const r = await autoDispatch(orderId)
    lastResult.value[orderId] = r
    return r
  }

  async function manual(orderId: number | string, installerId: number | string, remark?: string) {
    return await manualDispatch(orderId, installerId, remark)
  }

  return { candidates, lastResult, fetchCandidates, auto, manual }
})