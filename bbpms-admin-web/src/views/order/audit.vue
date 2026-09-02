<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOrderDetail, auditOrder } from '@/api/order'
import { formatDate, maskName, maskPhone } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const order = ref<any>(null)

const form = reactive({
  pass: true,
  remark: ''
})

async function fetchOrder() {
  loading.value = true
  try {
    const id = route.query.id as string
    if (!id) {
      ElMessage.error('Missing order id')
      router.back()
      return
    }
    order.value = await getOrderDetail(id)
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  if (!order.value) return
  submitting.value = true
  try {
    await auditOrder(order.value.id, { pass: form.pass, remark: form.remark })
    ElMessage.success(form.pass ? 'Approved' : 'Rejected')
    router.push(`/order/detail/${order.value.id}`)
  } finally {
    submitting.value = false
  }
}

onMounted(fetchOrder)
</script>

<template>
  <div class="app-container" v-loading="loading">
    <PageHeader title="Audit Order" description="Review and approve/reject the order" />

    <div class="app-card">
      <h3>Order Summary</h3>
      <el-descriptions v-if="order" :column="2" border>
        <el-descriptions-item label="Order No">{{ order.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="Status">{{ order.status }}</el-descriptions-item>
        <el-descriptions-item label="Customer">{{ maskName(order.customerName) }}</el-descriptions-item>
        <el-descriptions-item label="Phone">{{ maskPhone(order.customerPhone) }}</el-descriptions-item>
        <el-descriptions-item label="Package">{{ order.packageName }}</el-descriptions-item>
        <el-descriptions-item label="CS">{{ order.csName }}</el-descriptions-item>
        <el-descriptions-item label="Address" :span="2">{{ order.address }}</el-descriptions-item>
        <el-descriptions-item label="Appointment" :span="2">{{ formatDate(order.appointmentAt) }}</el-descriptions-item>
        <el-descriptions-item label="Remark" :span="2">{{ order.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <div class="app-card">
      <h3>Audit Decision</h3>
      <el-form :model="form" label-width="120px" style="max-width: 600px">
        <el-form-item label="Decision">
          <el-radio-group v-model="form.pass">
            <el-radio :value="true">Approve</el-radio>
            <el-radio :value="false">Reject</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="Remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="Audit remark (optional)" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="onSubmit">Confirm</el-button>
          <el-button @click="router.back()">Back</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>