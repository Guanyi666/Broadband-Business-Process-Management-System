<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getWorkorderDetail, completeWorkorder, cancelWorkorder, reassignWorkOrder } from '@/api/workorder'
import { formatDate } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import BBPMSStatusTag from '@/components/BBPMSStatusTag.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref<any>(null)

async function fetchData() {
  loading.value = true
  try {
    detail.value = await getWorkorderDetail(route.params.id as string)
  } finally {
    loading.value = false
  }
}

async function onComplete() {
  try {
    await ElMessageBox.confirm('Mark as completed?', 'Confirm', { type: 'success' })
  } catch { return }
  await completeWorkorder(detail.value.id)
  ElMessage.success('Completed')
  fetchData()
}

async function onCancel() {
  try {
    const { value } = await ElMessageBox.prompt('Cancel reason?', 'Cancel', { inputPattern: /.+/, inputErrorMessage: 'Required' })
    await cancelWorkorder(detail.value.id, value)
    ElMessage.success('Cancelled')
    fetchData()
  } catch {/* canceled */}
}

// 转派给指定装维 → backend reassign (WorkOrderVO{newInstallerId, reason}).
// The bare /transfer endpoint only returns the order to the pool.
async function onReassign() {
  try {
    const { value: target } = await ElMessageBox.prompt('Target installer ID?', 'Reassign', { inputPattern: /\S+/, inputErrorMessage: 'Required' })
    const { value: reason } = await ElMessageBox.prompt('Reassign reason?', 'Reassign', { inputPattern: /\S+/, inputErrorMessage: 'Required' })
    await reassignWorkOrder(detail.value.id, { newInstallerId: Number(target), reason })
    ElMessage.success('Reassigned')
    fetchData()
  } catch {/* canceled */}
}

onMounted(fetchData)
</script>

<template>
  <div class="app-container" v-loading="loading">
    <PageHeader :title="`Workorder ${detail?.workNo || ''}`">
      <template #extra>
        <el-button type="primary" @click="onComplete" v-if="detail?.status === 'IN_PROGRESS'">Complete</el-button>
        <el-button @click="onReassign" v-if="['DISPATCHED', 'ACCEPTED', 'IN_PROGRESS', 'STALLED'].includes(detail?.status)">Reassign</el-button>
        <el-button type="danger" plain @click="onCancel" v-if="['PENDING', 'DISPATCHED', 'ACCEPTED'].includes(detail?.status)">Cancel</el-button>
      </template>
    </PageHeader>

    <div class="app-card">
      <h3>Workorder Info</h3>
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="Workorder No">{{ detail.workNo }}</el-descriptions-item>
        <el-descriptions-item label="Status">
          <BBPMSStatusTag :status="detail.status" :label="detail.statusDesc || detail.status" />
        </el-descriptions-item>
        <el-descriptions-item label="Order ID">{{ detail.orderId }}</el-descriptions-item>
        <el-descriptions-item label="Installer">{{ detail.installerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Dispatcher">{{ detail.dispatcherName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Customer Phone">{{ detail.customerPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Package">{{ detail.packageName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Address">{{ detail.installAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Dispatched">{{ formatDate(detail.dispatchTime) }}</el-descriptions-item>
        <el-descriptions-item label="Accepted">{{ formatDate(detail.acceptTime) }}</el-descriptions-item>
        <el-descriptions-item label="Started">{{ formatDate(detail.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="Completed">{{ formatDate(detail.finishTime) }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>

<style scoped lang="scss">
</style>
