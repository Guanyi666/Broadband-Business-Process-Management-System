<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMetrics } from '@/api/monitor'
import { formatDate } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'

const metrics = ref<any>(null)
const loading = ref(false)
const grafanaUrl = ref(import.meta.env.VITE_GRAFANA_URL || '')

async function fetchData() {
  loading.value = true
  try {
    metrics.value = await getMetrics()
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<template>
  <div class="app-container" v-loading="loading">
    <PageHeader title="System Metrics" />

    <div class="metric-grid">
      <div class="metric-card">
        <div class="metric-label">CPU</div>
        <div class="metric-value">{{ metrics?.cpu ?? '-' }}%</div>
      </div>
      <div class="metric-card">
        <div class="metric-label">Memory</div>
        <div class="metric-value">{{ metrics?.memory ?? '-' }}%</div>
      </div>
      <div class="metric-card">
        <div class="metric-label">Disk</div>
        <div class="metric-value">{{ metrics?.disk ?? '-' }}%</div>
      </div>
      <div class="metric-card">
        <div class="metric-label">JVM Heap</div>
        <div class="metric-value">
          {{ metrics?.jvmHeapUsed ?? '-' }} / {{ metrics?.jvmHeapMax ?? '-' }} MB
        </div>
      </div>
      <div class="metric-card">
        <div class="metric-label">Threads</div>
        <div class="metric-value">{{ metrics?.threads ?? '-' }}</div>
      </div>
      <div class="metric-card">
        <div class="metric-label">Online Installers</div>
        <div class="metric-value">{{ metrics?.onlineInstallers ?? '-' }}</div>
      </div>
      <div class="metric-card">
        <div class="metric-label">Pending Orders</div>
        <div class="metric-value">{{ metrics?.pendingOrders ?? '-' }}</div>
      </div>
      <div class="metric-card">
        <div class="metric-label">Running Workorders</div>
        <div class="metric-value">{{ metrics?.runningWorkorders ?? '-' }}</div>
      </div>
    </div>

    <div class="app-card mt-16">
      <div class="flex-between mb-16">
        <div class="chart-title">Grafana Dashboard</div>
        <div class="text-muted">{{ formatDate(metrics?.timestamp) }}</div>
      </div>
      <div v-if="grafanaUrl" class="grafana-frame">
        <iframe :src="grafanaUrl" frameborder="0" width="100%" height="540" />
      </div>
      <el-empty v-else description="Configure VITE_GRAFANA_URL to embed dashboard" :image-size="80" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.metric-card {
  background: #fff;
  border-radius: $radius-base;
  padding: 20px;
  text-align: center;
  box-shadow: $shadow-card;
  .metric-label {
    color: #909399;
    font-size: 13px;
  }
  .metric-value {
    font-size: 24px;
    font-weight: 700;
    color: $primary-color;
    margin-top: 8px;
  }
}
.grafana-frame {
  background: #f5f7fa;
  border-radius: $radius-base;
  overflow: hidden;
}
@media (max-width: 1280px) {
  .metric-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>