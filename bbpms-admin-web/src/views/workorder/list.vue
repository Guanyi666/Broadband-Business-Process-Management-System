<script setup lang="ts">
import { ref, onMounted, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import { pageWorkorders } from '@/api/workorder'
import type { WorkorderItem } from '@/types/order'
import { formatDate } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import BBPMSStatusTag from '@/components/BBPMSStatusTag.vue'

const router = useRouter()

const loading = ref(false)
const list = ref<WorkorderItem[]>([])
const total = ref(0)
const activeTab = ref<'ALL' | WorkorderItem['status']>('ALL')

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

function onRowClick(row: WorkorderItem) {
  router.push(`/workorder/detail/${row.id}`)
}

function onSearch() { query.pageNum = 1; fetchData() }
function onReset() { query.installerId = ''; query.dateRange = []; query.pageNum = 1; fetchData() }

onMounted(fetchData)
</script>

<template>
  <div class="app-container">
    <PageHeader title="Workorders">
      <template #extra>
        <el-button type="primary" @click="router.push('/workorder/dispatch-board')">
          <el-icon><Connection /></el-icon> Dispatch Board
        </el-button>
      </template>
    </PageHeader>

    <div class="app-card">
      <el-tabs v-model="activeTab" class="mb-16">
        <el-tab-pane label="All" name="ALL" />
        <el-tab-pane label="Pending" name="PENDING" />
        <el-tab-pane label="Dispatched" name="DISPATCHED" />
        <el-tab-pane label="In Progress" name="IN_PROGRESS" />
        <el-tab-pane label="Completed" name="COMPLETED" />
        <el-tab-pane label="Cancelled" name="CANCELLED" />
      </el-tabs>

      <div class="page-toolbar">
        <div class="flex" style="gap: 8px">
          <el-input v-model="query.installerId" placeholder="Installer ID" clearable style="width: 200px" />
          <el-date-picker v-model="query.dateRange" type="daterange" range-separator="-" start-placeholder="From" end-placeholder="To" />
          <el-button type="primary" @click="onSearch">Search</el-button>
          <el-button @click="onReset">Reset</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" stripe @row-click="onRowClick">
        <el-table-column prop="workNo" label="Workorder No" width="180" />
        <el-table-column prop="orderId" label="Order ID" width="120" />
        <el-table-column label="Status" width="130">
          <template #default="{ row }"><BBPMSStatusTag :status="row.status" :label="row.statusDesc || row.status" /></template>
        </el-table-column>
        <el-table-column prop="installerName" label="Installer" width="120" />
        <el-table-column prop="customerPhone" label="Customer Phone" width="130" />
        <el-table-column label="Dispatch" width="170">
          <template #default="{ row }">{{ formatDate(row.dispatchTime, 'YYYY-MM-DD HH:mm') }}</template>
        </el-table-column>
        <el-table-column label="Created" width="170">
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
