<script setup lang="ts">
import { ref, onMounted, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import { pageOrders, cancelOrder } from '@/api/order'
import type { OrderItem, OrderStatus } from '@/types/order'
import { formatDate } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import BBPMSStatusTag from '@/components/BBPMSStatusTag.vue'

const router = useRouter()

const loading = ref(false)
const list = ref<OrderItem[]>([])
const total = ref(0)

const activeTab = ref<'ALL' | OrderStatus>('ALL')

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  dateRange: [] as string[]
})

const statusOptions: { label: string; value: OrderStatus }[] = [
  { label: 'Pending', value: 'PENDING' },
  { label: 'Approved', value: 'AUDIT_PASS' },
  { label: 'Rejected', value: 'AUDIT_REJECT' },
  { label: 'Dispatched', value: 'DISPATCHED' },
  { label: 'Installing', value: 'INSTALLING' },
  { label: 'Done', value: 'DONE' },
  { label: 'Cancelled', value: 'CANCELLED' }
]

async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword
    }
    if (activeTab.value !== 'ALL') params.status = activeTab.value
    if (query.dateRange?.length === 2) params.dateRange = query.dateRange
    const res = await pageOrders(params)
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

watch(activeTab, () => {
  query.pageNum = 1
  fetchData()
})

function onSearch() {
  query.pageNum = 1
  fetchData()
}

function onReset() {
  query.keyword = ''
  query.dateRange = []
  query.pageNum = 1
  fetchData()
}

function onAudit(row: OrderItem) {
  router.push({ path: '/order/audit', query: { id: row.id } })
}

async function onCancel(row: OrderItem) {
  try {
    await ElMessageBox.confirm(`Cancel order ${row.orderNo}?`, 'Confirm', { type: 'warning' })
  } catch { return }
  await cancelOrder(row.id)
  ElMessage.success('Cancelled')
  fetchData()
}

function onRowClick(row: OrderItem) {
  router.push(`/order/detail/${row.id}`)
}

onMounted(fetchData)
</script>

<template>
  <div class="app-container">
    <PageHeader title="Orders">
      <template #extra>
        <el-button type="primary" @click="router.push('/order/create')">
          <el-icon><Plus /></el-icon> Create Order
        </el-button>
      </template>
    </PageHeader>

    <div class="app-card">
      <el-tabs v-model="activeTab" class="mb-16">
        <el-tab-pane label="All" name="ALL" />
        <el-tab-pane label="Pending" name="PENDING" />
        <el-tab-pane label="Approved" name="AUDIT_PASS" />
        <el-tab-pane label="Installing" name="INSTALLING" />
        <el-tab-pane label="Done" name="DONE" />
      </el-tabs>

      <div class="page-toolbar">
        <div class="flex" style="gap: 8px; flex-wrap: wrap">
          <el-input v-model="query.keyword" placeholder="Order No / Customer" clearable style="width: 240px" @keyup.enter="onSearch" />
          <el-date-picker v-model="query.dateRange" type="daterange" range-separator="-" start-placeholder="From" end-placeholder="To" />
          <el-button type="primary" @click="onSearch">Search</el-button>
          <el-button @click="onReset">Reset</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" stripe @row-click="onRowClick">
        <el-table-column prop="orderNo" label="Order No" width="180" />
        <el-table-column prop="customerName" label="Customer" width="120" />
        <el-table-column prop="packageName" label="Package" min-width="160" show-overflow-tooltip />
        <el-table-column label="Status" width="120">
          <template #default="{ row }"><BBPMSStatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="Appointment" width="170">
          <template #default="{ row }">{{ formatDate(row.appointmentAt, 'YYYY-MM-DD HH:mm') }}</template>
        </el-table-column>
        <el-table-column prop="csName" label="CS" width="100" />
        <el-table-column label="Created" width="170">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="Action" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="onAudit(row)" v-if="row.status === 'PENDING'">Audit</el-button>
            <el-button link type="danger" @click.stop="onCancel(row)" v-if="['PENDING','AUDIT_PASS'].includes(row.status)">Cancel</el-button>
            <el-button link @click.stop="onRowClick(row)">Detail</el-button>
          </template>
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