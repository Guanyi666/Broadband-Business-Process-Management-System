<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import {
  handleServiceTicket, pageCustomerSubmittedOrders, pageProfileChanges, pageServiceTickets,
  reviewCustomerOrder, reviewProfileChange,
  type CustomerOrder, type ProfileChange, type ServiceTicket
} from '@/api/customerPortal'
import { formatDate } from '@/utils/format'

const activeTab = ref('orders')
const loading = ref(false)
const orders = ref<CustomerOrder[]>([])
const tickets = ref<ServiceTicket[]>([])
const changes = ref<ProfileChange[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 20, status: '' })

const orderStatusOptions = [
  { label: '待客服确认', value: 'PENDING_CS_CONFIRM' },
  { label: '客服已退回', value: 'CS_REJECTED' }
]
const ticketStatusOptions = ['SUBMITTED', 'ACCEPTED', 'PROCESSING', 'WAIT_CONFIRM', 'CLOSED', 'REJECTED']

async function load() {
  loading.value = true
  try {
    if (activeTab.value === 'orders') {
      const page = await pageCustomerSubmittedOrders(query)
      orders.value = page.records || []
      total.value = page.total
    } else if (activeTab.value === 'tickets') {
      const page = await pageServiceTickets(query)
      tickets.value = page.records || []
      total.value = page.total
    } else {
      const page = await pageProfileChanges(query)
      changes.value = page.records || []
      total.value = page.total
    }
  } finally { loading.value = false }
}

function switchTab() {
  query.status = ''
  query.pageNum = 1
  load()
}

async function reviewOrder(row: CustomerOrder, approved: boolean) {
  const { value } = await ElMessageBox.prompt(
    approved ? '可填写受理备注' : '请填写退回原因', approved ? '确认受理' : '退回订单',
    { inputPlaceholder: approved ? '选填' : '必填', inputValidator: (v) => approved || !!v || '请填写退回原因' }
  )
  await reviewCustomerOrder(row.id, approved, value)
  ElMessage.success(approved ? '已确认受理' : '已退回客户修改')
  load()
}

async function updateTicket(row: ServiceTicket, status: string) {
  const { value } = await ElMessageBox.prompt('填写处理结果或备注', `更新为 ${status}`, {
    inputValue: row.handleResult || '', inputPlaceholder: '处理说明'
  })
  await handleServiceTicket(row.id, { status, handleResult: value })
  ElMessage.success('服务单已更新')
  load()
}

async function reviewChange(row: ProfileChange, approved: boolean) {
  const { value } = await ElMessageBox.prompt('填写审核意见', approved ? '通过资料变更' : '驳回资料变更', {
    inputPlaceholder: '选填'
  })
  await reviewProfileChange(row.id, approved, value)
  ElMessage.success('审核完成')
  load()
}

onMounted(load)
</script>

<template>
  <div class="app-container">
    <PageHeader title="客户自助业务" description="处理客户自助报装、报障投诉和资料变更申请" />
    <div class="app-card">
      <el-tabs v-model="activeTab" @tab-change="switchTab">
        <el-tab-pane label="自助报装" name="orders" />
        <el-tab-pane label="报障/投诉" name="tickets" />
        <el-tab-pane label="资料审核" name="profile" />
      </el-tabs>

      <div class="page-toolbar">
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 180px" @change="load">
          <el-option v-if="activeTab === 'orders'" v-for="o in orderStatusOptions" :key="o.value" :label="o.label" :value="o.value" />
          <el-option v-else-if="activeTab === 'tickets'" v-for="s in ticketStatusOptions" :key="s" :label="s" :value="s" />
          <template v-else>
            <el-option label="待审核" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
          </template>
        </el-select>
        <el-button type="primary" @click="load">刷新</el-button>
      </div>

      <el-table v-if="activeTab === 'orders'" v-loading="loading" :data="orders" stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="190" />
        <el-table-column prop="packageName" label="套餐" min-width="150" />
        <el-table-column prop="installAddress" label="安装地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="statusLabel" label="状态" width="120" />
        <el-table-column label="提交时间" width="170"><template #default="{ row }">{{ formatDate(row.createTime) }}</template></el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING_CS_CONFIRM'">
              <el-button link type="success" @click="reviewOrder(row, true)">确认</el-button>
              <el-button link type="danger" @click="reviewOrder(row, false)">退回</el-button>
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>

      <el-table v-else-if="activeTab === 'tickets'" v-loading="loading" :data="tickets" stripe>
        <el-table-column prop="ticketNo" label="服务单号" min-width="190" />
        <el-table-column prop="type" label="类型" width="100" />
        <el-table-column prop="customerId" label="客户ID" width="100" />
        <el-table-column prop="description" label="问题描述" min-width="260" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="130" />
        <el-table-column label="操作" min-width="280" fixed="right">
          <template #default="{ row }">
            <el-button link @click="updateTicket(row, 'ACCEPTED')">受理</el-button>
            <el-button link type="primary" @click="updateTicket(row, 'PROCESSING')">处理中</el-button>
            <el-button link type="success" @click="updateTicket(row, 'WAIT_CONFIRM')">解决待确认</el-button>
            <el-button link type="danger" @click="updateTicket(row, 'REJECTED')">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-table v-else v-loading="loading" :data="changes" stripe>
        <el-table-column prop="customerId" label="客户ID" width="100" />
        <el-table-column prop="changeFields" label="变更字段" width="150" />
        <el-table-column prop="proposedName" label="姓名" width="110" />
        <el-table-column prop="proposedPhone" label="手机号" width="140" />
        <el-table-column prop="proposedAddress" label="地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="110" />
        <el-table-column label="申请时间" width="170"><template #default="{ row }">{{ formatDate(row.createTime) }}</template></el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button link type="success" @click="reviewChange(row, true)">通过</el-button>
              <el-button link type="danger" @click="reviewChange(row, false)">驳回</el-button>
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > query.pageSize"
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        style="margin-top: 18px; justify-content: flex-end"
        layout="total, prev, pager, next"
        :total="total"
        @current-change="load"
      />
    </div>
  </div>
</template>
