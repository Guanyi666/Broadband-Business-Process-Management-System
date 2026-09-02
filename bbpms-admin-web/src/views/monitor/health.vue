<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { getServiceHealth } from '@/api/monitor'
import { formatDate } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import BBPMSStatusTag from '@/components/BBPMSStatusTag.vue'

const loading = ref(false)
const list = ref<any[]>([])

async function fetchData() {
  loading.value = true
  try {
    list.value = await getServiceHealth()
  } finally {
    loading.value = false
  }
}

let timer: ReturnType<typeof setInterval> | null = null
onMounted(() => {
  fetchData()
  timer = setInterval(fetchData, 30000)
})
onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div class="app-container">
    <PageHeader title="Service Health">
      <template #extra>
        <el-button @click="fetchData"><el-icon><Refresh /></el-icon> Refresh</el-button>
      </template>
    </PageHeader>

    <div class="app-card">
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="name" label="Service" width="200" />
        <el-table-column label="Status" width="100">
          <template #default="{ row }"><BBPMSStatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column prop="host" label="Host" width="200" />
        <el-table-column prop="port" label="Port" width="80" />
        <el-table-column prop="message" label="Message" min-width="220" show-overflow-tooltip />
        <el-table-column label="Checked" width="180">
          <template #default="{ row }">{{ formatDate(row.checkedAt) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>