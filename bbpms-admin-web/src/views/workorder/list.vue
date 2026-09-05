<script setup lang="ts">
import { ref, onMounted, onActivated, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { pageWorkorders } from '@/api/workorder'
import type { WorkorderItem } from '@/types/order'
import { formatDate } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import BBPMSStatusTag from '@/components/BBPMSStatusTag.vue'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const list = ref<WorkorderItem[]>([])
const total = ref(0)
const statusOptions: { label: string; value: WorkorderItem['status'] }[] = [
  { label: '待派单', value: 'PENDING' },
  { label: '已派单', value: 'DISPATCHED' },
  { label: '已接单', value: 'ACCEPTED' },
  { label: '施工中', value: 'IN_PROGRESS' },
  { label: '已停滞', value: 'STALLED' },
  { label: '改派中', value: 'REASSIGNING' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '失败', value: 'FAILED' },
  { label: '已取消', value: 'CANCELLED' },
  { label: '自动取消', value: 'AUTO_CANCELLED' }
]

function statusFromRoute(value: unknown): 'ALL' | WorkorderItem['status'] {
  const status = Array.isArray(value) ? value[0] : value
  return statusOptions.some((item) => item.value === status) ? status as WorkorderItem['status'] : 'ALL'
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
          <el-tab-pane label="全部" name="ALL" />
          <el-tab-pane v-for="item in statusOptions" :key="item.value" :label="item.label" :name="item.value" />
        </el-tabs>

      <div class="page-toolbar">
        <div class="flex" style="gap: 8px">
          <el-input v-model="query.installerId" placeholder="Installer ID" clearable style="width: 200px" />
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
