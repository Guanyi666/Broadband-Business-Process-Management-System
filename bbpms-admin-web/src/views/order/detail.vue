<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderDetail, cancelOrder } from '@/api/order'
import { maskPhone, maskName, maskIdCard, formatDate } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import BBPMSStatusTag from '@/components/BBPMSStatusTag.vue'
import OrderTimeline from '@/components/OrderTimeline.vue'
import PermissionButton from '@/components/PermissionButton.vue'

const route = useRoute()
const router = useRouter()

const detail = ref<any>(null)
const loading = ref(false)

async function fetchData() {
  loading.value = true
  try {
    detail.value = await getOrderDetail(route.params.id as string)
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)

function onAudit() {
  router.push({ path: '/order/audit', query: { id: route.params.id } })
}

async function onCancel() {
  try {
    await ElMessageBox.confirm('Cancel this order?', '确认', { type: 'warning' })
  } catch { return }
  await cancelOrder(route.params.id as string)
  ElMessage.success('已取消')
  fetchData()
}

function onReassign() {
  router.push({ path: '/workorder/dispatch-board', query: { orderId: route.params.id } })
}

const canAudit = computed(() => detail.value?.status === 'CREATED')
const canCancel = computed(() => ['CREATED', 'AUDITED', 'WAIT_DISPATCH'].includes(detail.value?.status))
const canReassign = computed(() => ['AUDITED', 'WAIT_DISPATCH'].includes(detail.value?.status))
</script>

<template>
  <div class="app-container" v-loading="loading">
    <PageHeader :title="`Order ${detail?.orderNo || ''}`">
      <template #extra>
        <PermissionButton permission="order:audit">
          <el-button v-if="canAudit" type="primary" @click="onAudit">审核</el-button>
        </PermissionButton>
        <PermissionButton permission="order:cancel">
          <el-button v-if="canCancel" type="danger" plain @click="onCancel">取消</el-button>
        </PermissionButton>
        <PermissionButton permission="dispatch:manual">
          <el-button v-if="canReassign" @click="onReassign">转派</el-button>
        </PermissionButton>
      </template>
    </PageHeader>

    <div class="card-grid">
      <div class="app-card">
        <h3>订单信息</h3>
        <el-descriptions :column="2" border v-if="detail">
          <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态"><BBPMSStatusTag :status="detail.status" /></el-descriptions-item>
          <el-descriptions-item label="套餐">{{ detail.packageName }}</el-descriptions-item>
          <el-descriptions-item label="客服">{{ detail.csName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审核人">{{ detail.auditByName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审核时间">{{ formatDate(detail.auditAt) }}</el-descriptions-item>
          <el-descriptions-item label="地址" :span="2">{{ detail.address }}</el-descriptions-item>
          <el-descriptions-item label="预约时间" :span="2">{{ formatDate(detail.appointmentAt) }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">{{ formatDate(detail.createdAt) }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="app-card">
        <h3>客户</h3>
        <el-descriptions :column="1" border v-if="detail?.customer">
          <el-descriptions-item label="名称">{{ maskName(detail.customer.name) }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ maskPhone(detail.customer.phone) }}</el-descriptions-item>
          <el-descriptions-item label="身份证号">{{ maskIdCard(detail.customer.idCard) }}</el-descriptions-item>
          <el-descriptions-item label="地址">{{ detail.customer.address || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="app-card">
        <h3>工单信息</h3>
        <el-empty v-if="!detail?.workorder" description="Not dispatched yet" :image-size="60" />
        <el-descriptions v-else :column="1" border>
          <el-descriptions-item label="工单号">{{ detail.workorder.workorderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态"><BBPMSStatusTag :status="detail.workorder.status" /></el-descriptions-item>
          <el-descriptions-item label="装维人员">{{ detail.workorder.installerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="预约时间">{{ formatDate(detail.workorder.scheduledAt) }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </div>

    <OrderTimeline :events="detail?.timeline || []" title="订单时间线" />
  </div>
</template>

<style scoped lang="scss">
.card-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}
@media (max-width: 1280px) {
  .card-grid {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
