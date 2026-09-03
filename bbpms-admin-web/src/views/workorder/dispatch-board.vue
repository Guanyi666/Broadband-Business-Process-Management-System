<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageOrders } from '@/api/order'
import { listInstallerLocations } from '@/api/installer'
import { dispatchCandidates, autoDispatch, manualDispatch } from '@/api/dispatch'
import { formatDate } from '@/utils/format'
import type { OrderItem } from '@/types/order'
import type { DispatchCandidate } from '@/types/order'
import type { InstallerLocation } from '@/types/installer'
import PageHeader from '@/components/PageHeader.vue'
import BBPMSStatusTag from '@/components/BBPMSStatusTag.vue'

const loading = ref(false)
const pendingOrders = ref<OrderItem[]>([])
const onlineInstallers = ref<InstallerLocation[]>([])
const statusSummary = ref({ pending: 0, dispatched: 0, processing: 0, completed: 0 })

const dialogVisible = ref(false)
const dialogLoading = ref(false)
const currentOrder = ref<OrderItem | null>(null)
const candidates = ref<DispatchCandidate[]>([])

async function refresh() {
  loading.value = true
  try {
    const [orders, dispatched, processing, completed, locs] = await Promise.all([
      pageOrders({ pageNum: 1, pageSize: 50, status: 'WAIT_DISPATCH' }),
      pageOrders({ pageNum: 1, pageSize: 1, status: 'DISPATCHED' }),
      pageOrders({ pageNum: 1, pageSize: 1, status: 'INSTALLING' }),
      pageOrders({ pageNum: 1, pageSize: 1, status: 'FINISHED' }),
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

async function openDispatch(order: OrderItem) {
  currentOrder.value = order
  dialogVisible.value = true
  dialogLoading.value = true
  try {
    candidates.value = await dispatchCandidates(order.id, 10)
  } finally {
    dialogLoading.value = false
  }
}

async function onAutoDispatch() {
  if (!currentOrder.value) return
  try {
    await ElMessageBox.confirm('Auto-dispatch this order?', 'Confirm', { type: 'info' })
  } catch { return }
  try {
    const r = await autoDispatch(currentOrder.value.id)
    ElMessage.success(`Dispatched to ${r.installerName || r.installerId}`)
    dialogVisible.value = false
    refresh()
  } catch {
    /* error already toasted */
  }
}

async function onManualDispatch(c: DispatchCandidate) {
  if (!currentOrder.value) return
  try {
    await manualDispatch(currentOrder.value.id, c.installerId)
    ElMessage.success('派单成功')
    dialogVisible.value = false
    refresh()
  } catch {
    /* toast */
  }
}

let timer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  refresh()
  timer = setInterval(refresh, 30000)
})
onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div class="app-container" v-loading="loading">
    <PageHeader title="Dispatch Board">
      <template #extra>
        <el-button @click="refresh"><el-icon><Refresh /></el-icon> 刷新</el-button>
      </template>
    </PageHeader>

    <div class="status-row">
      <div class="status-card"><span>Pending Dispatch</span><strong>{{ statusSummary.pending }}</strong></div>
      <div class="status-card"><span>Dispatched</span><strong>{{ statusSummary.dispatched }}</strong></div>
      <div class="status-card"><span>Processing</span><strong>{{ statusSummary.processing }}</strong></div>
      <div class="status-card"><span>Completed</span><strong>{{ statusSummary.completed }}</strong></div>
    </div>

    <div class="board">
      <div class="board-col">
        <div class="board-col-header">
          <span>待处理订单</span>
          <el-tag size="small">{{ pendingOrders.length }}</el-tag>
        </div>
        <div class="board-col-body">
          <div v-for="o in pendingOrders" :key="o.id" class="order-card" @click="openDispatch(o)">
            <div class="order-no">{{ o.orderNo }}</div>
            <div class="order-meta">{{ o.customerName }} · {{ o.packageName }}</div>
            <div class="order-addr">{{ o.address }}</div>
            <div class="order-time">
              <el-icon><Clock /></el-icon>
              {{ formatDate(o.appointmentAt, 'MM-DD HH:mm') }}
            </div>
          </div>
          <el-empty v-if="!pendingOrders.length" description="No pending orders" :image-size="60" />
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
                {{ i.online ? 'Online' : 'Offline' }}
              </el-tag>
              <span>{{ i.name }}</span>
            </div>
            <div class="installer-stats">
              <span><el-icon><Tools /></el-icon> Workload: {{ i.workload ?? '-' }}</span>
              <span><el-icon><Star /></el-icon> {{ i.rating ?? '-' }}</span>
            </div>
            <div class="installer-loc text-muted" v-if="i.lng && i.lat">
              ({{ i.lng.toFixed(3) }}, {{ i.lat.toFixed(3) }})
            </div>
          </div>
          <el-empty v-if="!onlineInstallers.length" description="No installers online" :image-size="60" />
        </div>
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="`Dispatch - ${currentOrder?.orderNo || ''}`"
      width="640px"
    >
      <div v-loading="dialogLoading">
        <el-alert type="info" :closable="false" class="mb-16">
          <div>Customer: <strong>{{ currentOrder?.customerName }}</strong></div>
          <div>Address: {{ currentOrder?.address }}</div>
          <div>Appointment: {{ formatDate(currentOrder?.appointmentAt) }}</div>
        </el-alert>

        <div class="candidate-header">Top Candidates (by score)</div>
        <el-table :data="candidates" border>
          <el-table-column label="排名" width="60" type="index" />
          <el-table-column label="装维人员">
            <template #default="{ row }">{{ row.name }} ({{ row.phone || '-' }})</template>
          </el-table-column>
          <el-table-column label="综合评分" width="80">
            <template #default="{ row }">
              <el-tag :type="(row.score ?? 0) >= 80 ? 'success' : (row.score ?? 0) >= 60 ? 'warning' : 'info'">
                {{ row.score ?? '-' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="距离" width="100">
            <template #default="{ row }">{{ row.distanceKm?.toFixed?.(1) ?? '-' }} km</template>
          </el-table-column>
          <el-table-column label="当前负载" width="100">
            <template #default="{ row }">{{ row.workload ?? 0 }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="onManualDispatch(row)">分配</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="success" @click="onAutoDispatch">自动派单</el-button>
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
.candidate-header {
  font-weight: 600; margin-bottom: 8px;
}
</style>
