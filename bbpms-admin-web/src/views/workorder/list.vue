<script setup lang="ts">
import { ref, onMounted, onActivated, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { pageWorkorders } from '@/api/workorder'
import { getOverview } from '@/api/dashboard'
import type { WorkorderItem } from '@/types/order'
import { formatDate } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'
import PageHeader from '@/components/PageHeader.vue'
import BBPMSStatusTag from '@/components/BBPMSStatusTag.vue'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const loading = ref(false)
const list = ref<WorkorderItem[]>([])
const total = ref(0)

// ---------- 统计摘要（复用看板 overview 状态分布，存量全量口径） ----------
const summary = ref<Record<string, number>>({})
const summaryLoading = ref(false)

async function loadSummary() {
  if (!auth.hasPermission('dashboard:view')) return // 无看板权限则不显示统计
  summaryLoading.value = true
  try {
    const data = await getOverview(7)
    const dist: Record<string, number> = { ALL: 0 }
    for (const item of data.workOrderStatusDist ?? []) {
      dist[item.status] = Number(item.count || 0)
      dist.ALL += Number(item.count || 0)
    }
    summary.value = dist
  } catch {
    summary.value = {} // 静默降级，不阻断列表
  } finally {
    summaryLoading.value = false
  }
}

function summaryCount(key: string): number | null {
  return summaryLoading.value ? null : summary.value[key] ?? null
}

const summaryTabs: { key: string; label: string }[] = [
  { key: 'ALL', label: '全部' },
  { key: 'PENDING', label: '待派单' },
  { key: 'DISPATCHED', label: '已派单' },
  { key: 'ACCEPTED', label: '已接单' },
  { key: 'IN_PROGRESS', label: '施工中' },
  { key: 'STALLED', label: '已停滞' },
  { key: 'REASSIGNING', label: '改派中' },
  { key: 'COMPLETED', label: '已完成' },
  { key: 'FAILED', label: '失败' },
  { key: 'CANCELLED', label: '已取消' },
  { key: 'AUTO_CANCELLED', label: '自动取消' }
]

function statusFromRoute(value: unknown): 'ALL' | WorkorderItem['status'] {
  const status = Array.isArray(value) ? value[0] : value
  return summaryTabs.some((item) => item.key === status) ? status as WorkorderItem['status'] : 'ALL'
}

const activeTab = ref<'ALL' | WorkorderItem['status']>(statusFromRoute(route.query.status))

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  installerId: '',
  dateRange: [] as string[]
})

async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      installerId: query.installerId || undefined
    }
    if (activeTab.value !== 'ALL') params.status = activeTab.value
    // backend WorkOrderQueryReq filters by startTime/endTime, not dateRange
    if (query.dateRange?.length === 2) {
      params.startTime = query.dateRange[0]
      params.endTime = query.dateRange[1]
    }
    const res = await pageWorkorders(params)
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

watch(activeTab, () => { query.pageNum = 1; fetchData() })
watch(() => route.query.status, (value) => {
  const next = statusFromRoute(value)
  if (next !== activeTab.value) activeTab.value = next
})

function onRowClick(row: WorkorderItem) {
  router.push(`/workorder/detail/${row.id}`)
}

function onSearch() { query.pageNum = 1; fetchData() }
function onReset() { query.installerId = ''; query.dateRange = []; query.pageNum = 1; fetchData() }

onMounted(fetchData)
onActivated(fetchData)
</script>

<template>
  <div class="app-container">
    <PageHeader title="工单管理">
      <template #extra>
        <el-button type="primary" @click="router.push('/workorder/dispatch-board')">
          <el-icon><Connection /></el-icon> 派单工作台
        </el-button>
      </template>
    </PageHeader>

    <div class="app-card">
        <el-tabs v-model="activeTab" class="mb-16">
          <el-tab-pane
            v-for="item in summaryTabs"
            :key="item.key"
            :name="item.key"
          >
            <template #label>
              <span class="summary-tab">
                {{ item.label }}
                <span v-if="summaryCount(item.key) !== null" class="summary-tab__count">{{ summaryCount(item.key) }}</span>
              </span>
            </template>
          </el-tab-pane>
        </el-tabs>

      <div class="page-toolbar">
        <div class="flex" style="gap: 8px">
          <el-input v-model="query.installerId" placeholder="装维 ID" clearable style="width: 200px" />
          <el-date-picker v-model="query.dateRange" type="daterange" range-separator="-" start-placeholder="开始日期" end-placeholder="结束日期" />
          <el-button type="primary" @click="onSearch">搜索</el-button>
          <el-button @click="onReset">重置</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" stripe @row-click="onRowClick">
        <el-table-column prop="workNo" label="工单号" width="180" />
        <el-table-column prop="orderId" label="订单 ID" width="120" />
        <el-table-column label="状态" width="130">
          <template #default="{ row }"><BBPMSStatusTag :status="row.status" :label="row.statusDesc || row.status" /></template>
        </el-table-column>
        <el-table-column prop="installerName" label="装维人员" width="120" />
        <el-table-column prop="customerPhone" label="客户电话" width="130" />
        <el-table-column label="派单时间" width="170">
          <template #default="{ row }">{{ formatDate(row.dispatchTime, 'YYYY-MM-DD HH:mm') }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          background
          layout="total, prev, pager, next, jumper"
          :total="total"
          :current-page="query.pageNum"
          :page-size="query.pageSize"
          @current-change="(p) => { query.pageNum = p; fetchData() }"
          @size-change="(s) => { query.pageSize = s; query.pageNum = 1; fetchData() }"
        />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
// 统计摘要 Tab：标签 + 计数徽标
.summary-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;

  &__count {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 18px;
    height: 18px;
    padding: 0 5px;
    border-radius: 9px;
    background: var(--el-fill-color-light, #f5f7fa);
    color: var(--el-text-color-secondary, #909399);
    font-size: 12px;
    font-variant-numeric: tabular-nums;
  }

  .is-active &__count {
    background: var(--el-color-primary-light-9, #ecf5ff);
    color: var(--el-color-primary, #409eff);
  }
}
</style>
