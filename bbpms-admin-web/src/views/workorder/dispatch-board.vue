<script setup lang="ts">
import { computed, ref, onMounted, onActivated, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageWorkorders } from '@/api/workorder'
import { listInstallerLocations } from '@/api/installer'
import { dispatchCandidates, autoDispatch, manualDispatch } from '@/api/dispatch'
import { formatDate } from '@/utils/format'
import type { DispatchCandidate, WorkorderItem } from '@/types/order'
import type { InstallerLocation } from '@/types/installer'
import PageHeader from '@/components/PageHeader.vue'
import DispatchDecisionPanel from '@/components/DispatchDecisionPanel.vue'

const route = useRoute()

const loading = ref(false)
const pendingOrders = ref<WorkorderItem[]>([])
const onlineInstallers = ref<InstallerLocation[]>([])
const statusSummary = ref({ pending: 0, dispatched: 0, processing: 0, completed: 0 })

const dialogVisible = ref(false)
const dialogLoading = ref(false)
const currentOrder = ref<WorkorderItem | null>(null)
const candidates = ref<DispatchCandidate[]>([])
const selectedCandidateId = ref<number | string | null>(null)
const candidateError = ref('')
const actionLoading = ref<'auto' | 'manual' | ''>('')
const handledRouteOrder = ref(false)

const selectedCandidate = computed(() =>
  candidates.value.find((candidate) => String(candidate.installerId) === String(selectedCandidateId.value))
  || candidates.value[0]
  || null
)

async function refresh() {
  loading.value = true
  try {
    const [orders, dispatched, processing, completed, locs] = await Promise.all([
      pageWorkorders({ pageNum: 1, pageSize: 50, status: 'PENDING' }),
      pageWorkorders({ pageNum: 1, pageSize: 1, status: 'DISPATCHED' }),
      pageWorkorders({ pageNum: 1, pageSize: 1, status: 'IN_PROGRESS' }),
      pageWorkorders({ pageNum: 1, pageSize: 1, status: 'COMPLETED' }),
      listInstallerLocations().catch(() => [] as InstallerLocation[])
    ])
    pendingOrders.value = orders.list
    statusSummary.value = {
      pending: orders.total,
      dispatched: dispatched.total,
      processing: processing.total,
      completed: completed.total
    }
    onlineInstallers.value = (locs || []).filter((l) => l.online)
  } finally {
    loading.value = false
  }
}

async function openDispatch(order: WorkorderItem) {
  currentOrder.value = order
  candidates.value = []
  selectedCandidateId.value = null
  candidateError.value = ''
  dialogVisible.value = true
  dialogLoading.value = true
  try {
    candidates.value = await dispatchCandidates(order.orderId, 10)
    selectedCandidateId.value = candidates.value[0]?.installerId ?? null
  } catch (e: any) {
    candidateError.value = e?.response?.data?.message || e?.response?.data?.msg || e?.message || '候选装维加载失败'
  } finally {
    dialogLoading.value = false
  }
}

async function onAutoDispatch() {
  if (!currentOrder.value || !candidates.value.length) return
  try {
    await ElMessageBox.confirm(`确定按系统排名自动派单给 ${candidates.value[0].name} 吗？`, '自动派单确认', { type: 'info' })
  } catch { return }
  actionLoading.value = 'auto'
  try {
    const r = await autoDispatch(currentOrder.value.orderId)
    ElMessage.success(`已派单给 ${r.installerName || r.installerId}`)
    dialogVisible.value = false
    await refresh()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.response?.data?.msg || e?.message || '自动派单失败')
  } finally {
    actionLoading.value = ''
  }
}

async function onManualDispatch() {
  if (!currentOrder.value || !selectedCandidate.value) return
  const candidate = selectedCandidate.value
  try {
    await ElMessageBox.confirm(`确定人工选择 ${candidate.name} 承接该工单吗？`, '人工派单确认', { type: 'warning' })
  } catch { return }
  actionLoading.value = 'manual'
  try {
    await manualDispatch(currentOrder.value.orderId, candidate.installerId, `调度员从候选排名中选择第 ${candidates.value.indexOf(candidate) + 1} 名`)
    ElMessage.success(`已人工派单给 ${candidate.name}`)
    dialogVisible.value = false
    await refresh()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.response?.data?.msg || e?.message || '手动派单失败')
  } finally {
    actionLoading.value = ''
  }
}

let timer: ReturnType<typeof setInterval> | null = null

onMounted(async () => {
  await refresh()
  const targetOrderId = Array.isArray(route.query.orderId) ? route.query.orderId[0] : route.query.orderId
  const target = pendingOrders.value.find((order) => String(order.orderId) === String(targetOrderId || ''))
  if (target && !handledRouteOrder.value) {
    handledRouteOrder.value = true
    await openDispatch(target)
  }
  timer = setInterval(refresh, 30000)
})
onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
onActivated(refresh)
</script>

<template>
  <div class="app-container" v-loading="loading">
    <PageHeader title="派单工作台">
      <template #extra>
        <el-button @click="refresh"><el-icon><Refresh /></el-icon> 刷新</el-button>
      </template>
    </PageHeader>

    <div class="status-row">
      <div class="status-card"><span>待派单</span><strong>{{ statusSummary.pending }}</strong></div>
      <div class="status-card"><span>已派单</span><strong>{{ statusSummary.dispatched }}</strong></div>
      <div class="status-card"><span>施工中</span><strong>{{ statusSummary.processing }}</strong></div>
      <div class="status-card"><span>已完成</span><strong>{{ statusSummary.completed }}</strong></div>
    </div>

    <div class="board">
      <div class="board-col">
        <div class="board-col-header">
          <span>待派发工单</span>
          <el-tag size="small">{{ pendingOrders.length }}</el-tag>
        </div>
        <div class="board-col-body">
          <div v-for="o in pendingOrders" :key="o.id" class="order-card" @click="openDispatch(o)">
            <div class="order-no">{{ o.workNo }}</div>
            <div class="order-meta">{{ o.customerPhone || '-' }} · {{ o.packageName || '-' }}</div>
            <div class="order-addr">{{ o.installAddress || '-' }}</div>
            <div class="order-time">
              <el-icon><Clock /></el-icon>
              {{ formatDate(o.createTime, 'MM-DD HH:mm') }}
            </div>
          </div>
          <el-empty v-if="!pendingOrders.length" description="暂无待派发工单" :image-size="60" />
        </div>
      </div>

      <div class="board-col">
        <div class="board-col-header">
          <span>在线装维</span>
          <el-tag size="small" type="success">{{ onlineInstallers.length }}</el-tag>
        </div>
        <div class="board-col-body">
          <div v-for="i in onlineInstallers" :key="i.installerId" class="installer-card">
            <div class="installer-name">
              <el-tag size="small" :type="i.online ? 'success' : 'info'">
                {{ i.online ? '在线' : '离线' }}
              </el-tag>
              <span>{{ i.name }}</span>
            </div>
            <div class="installer-stats">
              <span><el-icon><Tools /></el-icon> 当前负载：{{ i.workload ?? '-' }}</span>
              <span><el-icon><Star /></el-icon> {{ i.rating ?? '-' }}</span>
            </div>
            <div class="installer-loc text-muted" v-if="i.lng && i.lat">
              ({{ i.lng.toFixed(3) }}, {{ i.lat.toFixed(3) }})
            </div>
          </div>
          <el-empty v-if="!onlineInstallers.length" description="暂无在线装维" :image-size="60" />
        </div>
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="`派单决策透视 - ${currentOrder?.workNo || ''}`"
      width="900px"
    >
      <div>
        <el-alert type="info" :closable="false" class="mb-16">
          <div>客户电话：<strong>{{ currentOrder?.customerPhone || '-' }}</strong></div>
          <div>安装地址：{{ currentOrder?.installAddress || '-' }}</div>
          <div>建单时间：{{ formatDate(currentOrder?.createTime) }}</div>
        </el-alert>

        <el-alert v-if="candidateError" type="error" :closable="false" class="mb-16" :title="candidateError" />
        <DispatchDecisionPanel
          :candidates="candidates"
          :selected-id="selectedCandidateId"
          :loading="dialogLoading"
          @select="(candidate) => selectedCandidateId = candidate.installerId"
        />
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          plain
          :disabled="!selectedCandidate"
          :loading="actionLoading === 'manual'"
          @click="onManualDispatch"
        >人工分配给{{ selectedCandidate?.name ? ` ${selectedCandidate.name}` : '' }}</el-button>
        <el-button
          type="success"
          :disabled="!candidates.length"
          :loading="actionLoading === 'auto'"
          @click="onAutoDispatch"
        >采用系统首选</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.board {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.status-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}
.status-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  background: #fff;
  border-radius: $radius-base;
  box-shadow: $shadow-card;
  color: #606266;
  strong { color: #303133; font-size: 24px; }
}
.board-col {
  background: #f5f7fa;
  border-radius: $radius-base;
  overflow: hidden;
  &-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    background: #fff;
    border-bottom: 1px solid #ebeef5;
    font-weight: 600;
  }
  &-body {
    padding: 12px;
    max-height: 70vh;
    overflow-y: auto;
  }
}
.order-card, .installer-card {
  background: #fff;
  border-radius: $radius-base;
  padding: 12px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: box-shadow 0.2s;
  &:hover { box-shadow: $shadow-card; }
}
.order-no { font-weight: 600; color: #303133; }
.order-meta { color: #909399; font-size: 12px; margin-top: 4px; }
.order-addr { color: #606266; font-size: 13px; margin-top: 4px; }
.order-time { color: #e6a23c; font-size: 12px; margin-top: 6px; display: flex; align-items: center; gap: 4px; }

.installer-name {
  display: flex; align-items: center; gap: 8px;
  font-weight: 600;
}
.installer-stats {
  display: flex; gap: 16px; font-size: 12px; color: #606266; margin-top: 6px;
}
.installer-loc { font-size: 11px; margin-top: 4px; }
</style>
