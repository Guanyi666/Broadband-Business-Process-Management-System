<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getInstallerProfile } from '@/api/installer'
import type { InstallerProfile } from '@/types/installer'
import { formatDate } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import BBPMSStatusTag from '@/components/BBPMSStatusTag.vue'

const route = useRoute()
const detail = ref<InstallerProfile | null>(null)
const loading = ref(false)

async function fetchData() {
  loading.value = true
  try {
    detail.value = await getInstallerProfile(route.params.id as string)
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<template>
  <div class="app-container" v-loading="loading">
    <PageHeader :title="detail?.name || 'Installer'" />
    <div class="app-card" v-if="detail">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="Status"><BBPMSStatusTag :status="detail.status" /></el-descriptions-item>
        <el-descriptions-item label="Username">{{ detail.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Name">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="Phone">{{ detail.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Workload">{{ detail.workload ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="Rating">{{ detail.rating ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="Last Active">{{ formatDate(detail.lastActiveAt) }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>