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
    await ElMessageBox.confirm('Cancel this order?', 'Confirm', { type: 'warning' })
  } catch { return }
  await cancelOrder(route.params.id as string)
  ElMessage.success('Cancelled')
  fetchData()
}

function onReassign() {
  router.push({ path: '/workorder/dispatch-board', query: { orderId: route.params.id } })
}

const canAudit = computed(() => detail.value?.status === 'PENDING')
const canCancel = computed(() => ['PENDING', 'AUDIT_PASS'].includes(detail.value?.status))
const canReassign = computed(() => detail.value?.status === 'AUDIT_PASS' && !detail.value?.workorder)
</script>

<template>
  <div class="app-container" v-loading="loading">
    <PageHeader :title="`Order ${detail?.orderNo || ''}`">
      <template #extra>
        <PermissionButton permission="order:audit">
          <el-button v-if="canAudit" type="primary" @click="onAudit">Audit</el-button>
        </PermissionButton>
        <PermissionButton permission="order:cancel">
          <el-button v-if="canCancel" type="danger" plain @click="onCancel">Cancel</el-button>
        </PermissionButton>
        <PermissionButton permission="dispatch:manual">
          <el-button v-if="canReassign" @click="onReassign">Dispatch</el-button>
        </PermissionButton>
      </template>
    </PageHeader>

    <div class="card-grid">
      <div class="app-card">
        <h3>Order Info</h3>
        <el-descriptions :column="2" border v-if="detail">
          <el-descriptions-item label="Order No">{{ detail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="Status"><BBPMSStatusTag :status="detail.status" /></el-descriptions-item>
          <el-descriptions-item label="Package">{{ detail.packageName }}</el-descriptions-item>
          <el-descriptions-item label="CS">{{ detail.csName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="Auditor">{{ detail.auditByName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="Audit Time">{{ formatDate(detail.auditAt) }}</el-descriptions-item>
          <el-descriptions-item label="Address" :span="2">{{ detail.address }}</el-descriptions-item>
          <el-descriptions-item label="Appointment" :span="2">{{ formatDate(detail.appointmentAt) }}</el-descriptions-item>
          <el-descriptions-item label="Remark" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="Created" :span="2">{{ formatDate(detail.createdAt) }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="app-card">
        <h3>Customer</h3>
        <el-descriptions :column="1" border v-if="detail?.customer">
          <el-descriptions-item label="Name">{{ maskName(detail.customer.name) }}</el-descriptions-item>
          <el-descriptions-item label="Phone">{{ maskPhone(detail.customer.phone) }}</el-descriptions-item>
          <el-descriptions-item label="ID Card">{{ maskIdCard(detail.customer.idCard) }}</el-descriptions-item>
          <el-descriptions-item label="Address">{{ detail.customer.address || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="app-card">
        <h3>Workorder</h3>
        <el-empty v-if="!detail?.workorder" description="Not dispatched yet" :image-size="60" />
        <el-descriptions v-else :column="1" border>
          <el-descriptions-item label="Workorder No">{{ detail.workorder.workorderNo }}</el-descriptions-item>
          <el-descriptions-item label="Status"><BBPMSStatusTag :status="detail.workorder.status" /></el-descriptions-item>
          <el-descriptions-item label="Installer">{{ detail.workorder.installerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="Scheduled">{{ formatDate(detail.workorder.scheduledAt) }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </div>

    <OrderTimeline :events="detail?.timeline || []" title="Order Timeline" />
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