<script setup lang="ts">
import { ref, onMounted, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import { pageOrders, cancelOrder, resubmitOrder } from '@/api/order'
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
  { label: '待审核', value: 'CREATED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '已审核', value: 'AUDITED' },
  { label: '待派单', value: 'WAIT_DISPATCH' },
  { label: '已派单', value: 'DISPATCHED' },
  { label: '安装中', value: 'INSTALLING' },
  { label: '已完成', value: 'FINISHED' },
  { label: '已归档', value: 'CLOSED' },
  { label: '已取消', value: 'CANCELLED' }
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
    if (query.dateRange?.length === 2) {
      params.startTime = `${query.dateRange[0]} 00:00:00`
      params.endTime = `${query.dateRange[1]} 23:59:59`
    }
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
    await ElMessageBox.confirm(`确定取消订单 ${row.orderNo} 吗？`, '确认', { type: 'warning' })
  } catch { return }
  await cancelOrder(row.id)
  ElMessage.success('已取消')
  fetchData()
}

async function onResubmit(row: OrderItem) {
  try {
    await ElMessageBox.confirm(`确定重新提交订单 ${row.orderNo} 吗？将重新进入审核队列。`, '确认', { type: 'warning' })
  } catch { return }
  await resubmitOrder(row.id)
  ElMessage.success('已重新提交，等待审核')
  fetchData()
}

function onRowClick(row: OrderItem) {
  router.push(`/order/detail/${row.id}`)
}

onMounted(fetchData)
</script>

<template>
  <div class="app-container">
    <PageHeader title="订单管理">
      <template #extra>
        <el-button type="primary" @click="router.push('/order/create')">
          <el-icon><Plus /></el-icon> 创建订单
        </el-button>
      </template>
    </PageHeader>

    <div class="app-card">
      <el-tabs v-model="activeTab" class="mb-16">
        <el-tab-pane label="全部" name="ALL" />
        <el-tab-pane label="待审核" name="CREATED" />
        <el-tab-pane label="已驳回" name="REJECTED" />
        <el-tab-pane label="待派单" name="WAIT_DISPATCH" />
        <el-tab-pane label="安装中" name="INSTALLING" />
        <el-tab-pane label="已完成" name="FINISHED" />
      </el-tabs>

      <div class="page-toolbar">
        <div class="flex" style="gap: 8px; flex-wrap: wrap">
          <el-input v-model="query.keyword" placeholder="订单号 / 客户" clearable style="width: 240px" @keyup.enter="onSearch" />
          <el-date-picker v-model="query.dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="-" start-placeholder="开始日期" end-placeholder="结束日期" />
          <el-button type="primary" @click="onSearch">搜索</el-button>
          <el-button @click="onReset">重置</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" stripe @row-click="onRowClick">
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="customerName" label="客户" width="120" />
        <el-table-column prop="packageName" label="套餐" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="120">
          <template #default="{ row }"><BBPMSStatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="预约时间" width="170">
          <template #default="{ row }">{{ formatDate(row.appointmentAt, 'YYYY-MM-DD HH:mm') }}</template>
        </el-table-column>
        <el-table-column prop="csName" label="客服" width="100" />
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="onAudit(row)" v-if="row.status === 'CREATED'">审核</el-button>
            <el-button link type="warning" @click.stop="onResubmit(row)" v-if="row.status === 'REJECTED'">重新提交</el-button>
            <el-button link type="danger" @click.stop="onCancel(row)" v-if="['CREATED','REJECTED','AUDITED','WAIT_DISPATCH'].includes(row.status)">取消</el-button>
            <el-button link @click.stop="onRowClick(row)">详情</el-button>
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
