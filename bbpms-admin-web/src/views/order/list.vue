<script setup lang="ts">
import { ref, onMounted, onActivated, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { pageOrders, cancelOrder, resubmitOrder } from '@/api/order'
import type { OrderItem, OrderStatus } from '@/types/order'
import { formatDate } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import BBPMSStatusTag from '@/components/BBPMSStatusTag.vue'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const loading = ref(false)
const list = ref<OrderItem[]>([])
const total = ref(0)
const actionLoading = ref('')

const validStatuses: OrderStatus[] = ['CREATED', 'REJECTED', 'AUDITED', 'WAIT_DISPATCH', 'DISPATCHED', 'INSTALLING', 'FINISHED', 'CLOSED', 'CANCELLED']

function statusFromRoute(value: unknown): 'ALL' | OrderStatus {
  const status = Array.isArray(value) ? value[0] : value
  return validStatuses.includes(status as OrderStatus) ? status as OrderStatus : 'ALL'
}

const activeTab = ref<'ALL' | OrderStatus>(statusFromRoute(route.query.status))

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

/** 套餐名称中文化兜底（后端已映射；此处兼容旧接口/缓存数据） */
const packageNameZh = (row: OrderItem): string => {
  const raw = row.packageName || ''
  const byCode: Record<string, string> = {
    PKG_100M: '100M 宽带', 'PKG-100M': '100M 宽带',
    PKG_300M: '300M 宽带', 'PKG-300M': '300M 宽带',
    PKG_500M: '500M 宽带', 'PKG-500M': '500M 宽带',
    PKG_1G: '1000M 宽带', 'PKG-1G': '1000M 宽带',
    PKG_1000M: '1000M 宽带', 'PKG-1000M': '1000M 宽带',
    FIBER_500M: '光纤 500M 宽带', 'FIBER-500M': '光纤 500M 宽带'
  }
  if (row.packageCode && byCode[row.packageCode]) return byCode[row.packageCode]
  const lower = raw.toLowerCase()
  if (lower.includes('宽带') || /[\u4e00-\u9fa5]/.test(raw)) return raw
  if (lower.includes('1g') || lower.includes('1000m')) return '1000M 宽带'
  if (lower.includes('100m')) return '100M 宽带'
  if (lower.includes('300m')) return '300M 宽带'
  if (lower.includes('500m')) return '500M 宽带'
  return raw
}

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

watch(() => route.query.status, (value) => {
  const next = statusFromRoute(value)
  if (next !== activeTab.value) activeTab.value = next
})

function onSearch() {
  // 时间区间校验：结束日期不能早于开始日期，跨度不超过 1 年（与后端一致）
  if (query.dateRange?.length === 2) {
    const [start, end] = query.dateRange
    if (start > end) {
      ElMessage.warning('结束日期不能早于开始日期')
      return
    }
    const maxDays = 366
    const diffDays = (new Date(end).getTime() - new Date(start).getTime()) / 86400000
    if (diffDays > maxDays) {
      ElMessage.warning('查询时间跨度不能超过 1 年')
      return
    }
  }
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
  actionLoading.value = `cancel-${row.id}`
  try {
    await cancelOrder(row.id)
    ElMessage.success('已取消')
    await fetchData()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.response?.data?.msg || e?.message || '取消订单失败')
  } finally {
    actionLoading.value = ''
  }
}

async function onResubmit(row: OrderItem) {
  try {
    await ElMessageBox.confirm(`确定重新提交订单 ${row.orderNo} 吗？将重新进入审核队列。`, '确认', { type: 'warning' })
  } catch { return }
  actionLoading.value = `resubmit-${row.id}`
  try {
    await resubmitOrder(row.id)
    ElMessage.success('已重新提交，等待审核')
    await fetchData()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.response?.data?.msg || e?.message || '重新提交失败')
  } finally {
    actionLoading.value = ''
  }
}

function onRowClick(row: OrderItem) {
  router.push(`/order/detail/${row.id}`)
}

onMounted(fetchData)
onActivated(fetchData)
</script>

<template>
  <div class="app-container">
    <PageHeader title="订单管理">
      <template #extra>
        <el-button v-if="auth.hasPermission('order:create')" type="primary" @click="router.push('/order/create')">
          <el-icon><Plus /></el-icon> 创建订单
        </el-button>
      </template>
    </PageHeader>

    <div class="app-card">
        <el-tabs v-model="activeTab" class="mb-16">
          <el-tab-pane label="全部" name="ALL" />
          <el-tab-pane v-for="item in statusOptions" :key="item.value" :label="item.label" :name="item.value" />
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
        <el-table-column prop="packageName" label="套餐" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ packageNameZh(row) }}</template>
        </el-table-column>
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
            <el-button link type="primary" @click.stop="onAudit(row)" v-if="row.status === 'CREATED' && auth.hasPermission('order:audit')">审核</el-button>
            <el-button
              v-if="row.status === 'REJECTED' && auth.hasPermission('order:create')"
              link
              type="warning"
              :loading="actionLoading === `resubmit-${row.id}`"
              @click.stop="onResubmit(row)"
            >重新提交</el-button>
            <el-button
              v-if="['CREATED','REJECTED'].includes(row.status) && auth.hasPermission('order:cancel')"
              link
              type="danger"
              :loading="actionLoading === `cancel-${row.id}`"
              @click.stop="onCancel(row)"
            >取消</el-button>
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
