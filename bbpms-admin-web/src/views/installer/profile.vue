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
    <PageHeader :title="detail?.name || '装维人员'" />
    <div class="app-card" v-if="detail">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="状态"><BBPMSStatusTag :status="detail.status" /></el-descriptions-item>
        <el-descriptions-item label="用户名">{{ detail.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detail.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="当前负载">{{ detail.workload ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="评分">{{ detail.rating ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="最后活跃">{{ formatDate(detail.lastActiveAt) }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>