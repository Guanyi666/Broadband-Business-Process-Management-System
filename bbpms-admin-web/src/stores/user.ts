import { defineStore } from 'pinia'
import { ref } from 'vue'
import { pageUsers, createUser, updateUser, deleteUser, assignRoles } from '@/api/user'
import type { SysUser, UserCreatePayload } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const list = ref<SysUser[]>([])
  const total = ref(0)
  const loading = ref(false)
  const query = ref<Record<string, any>>({ pageNum: 1, pageSize: 10 })

  async function fetchPage() {
    loading.value = true
    try {
      const res = await pageUsers(query.value)
      list.value = res.list
      total.value = res.total
    } finally {
      loading.value = false
    }
  }

  async function create(payload: UserCreatePayload) {
    await createUser(payload)
  }

  async function update(id: number | string, payload: Partial<UserCreatePayload>) {
    await updateUser(id, payload)
  }

  async function remove(id: number | string) {
    await deleteUser(id)
  }

  async function assign(id: number | string, roleIds: (number | string)[]) {
    await assignRoles(id, roleIds)
  }

  function resetQuery() {
    query.value = { pageNum: 1, pageSize: 10 }
  }

  return { list, total, loading, query, fetchPage, create, update, remove, assign, resetQuery }
})